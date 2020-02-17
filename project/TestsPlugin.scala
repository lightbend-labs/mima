package mimabuild

import java.net.URLClassLoader
import com.typesafe.config.ConfigFactory
import sbt.{ Console => _, _ }
import sbt.Keys._
import sbt.librarymanagement.{ DependencyResolution, UnresolvedWarningConfiguration, UpdateConfiguration }

object TestsPlugin extends AutoPlugin {
  object autoImport {
    // This is the key for the scala version used to compile the tests, so that we can cross test the MiMa version
    // actually being used in the sbt plugin against multiple scala compiler versions.
    // Also the base project has dependencies that don't resolve under newer versions of scala.
    val testScalaVersion = settingKey[String]("The scala version to use to compile the test classes")

    val testFunctional = taskKey[Unit]("Run the functional test")

    val testCollectProblems = taskKey[Unit]("Test collecting problems")
    val testAppRun          = taskKey[Unit]("Test running the App, using library v2")
  }
  import autoImport._

  override def extraProjects = funTestProjects ++ intTestProjects

  override def buildSettings = Seq(
    resolvers += "scala-pr-validation-snapshots" at "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/",
    resolvers += "scala-integration" at "https://scala-ci.typesafe.com/artifactory/scala-integration/",
    testScalaVersion := sys.props.getOrElse("mima.testScalaVersion", scalaVersion.value),
  )

  override def projectSettings = Seq(
            testFunctional := dependOnAll(funTestProjects, Test / test).value,
       testCollectProblems := dependOnAll(funTestProjects, testCollectProblems).value,
                testAppRun := dependOnAll(funTestProjects, testAppRun).value,
    IntegrationTest / test := dependOnAll(intTestProjects, IntegrationTest / test).value,
  )

  private val functionalTests = LocalProject("functional-tests")

  private val V1   = config("v1").extend(Compile)   // Version 1 of a library
  private val V2   = config("v2").extend(Compile)   // Version 2 of a library
  private val App  = config("app").extend(V1)       // An App, built against library v1

  private def testProjects(prefix: String, fileName: String, setup: Project => Project) = {
    (file("functional-tests") / "src" / prefix * dirContaining(fileName)).get().map { base =>
      Project(s"$prefix-${base.name}", base).configure(setup)
    }
  }

  private def intTestProject(p: Project) = p.settings(IntegrationTest / test := runIntegrationTest.value)
  private def funTestProject(p: Project) = p.settings(funTestProjectSettings).configs(V1, V2, App)

  private lazy val funTestProjects = testProjects("test", "problems.txt", funTestProject)
  private lazy val intTestProjects = testProjects( "it" ,   "test.conf" , intTestProject)

  private val oracleFile = Def.task {
    val p    = baseDirectory.value / "problems.txt"
    val p211 = baseDirectory.value / "problems-2.11.txt"
    val p212 = baseDirectory.value / "problems-2.12.txt"
    scalaVersion.value.take(4) match {
      case "2.11" => if (p211.exists()) p211 else if (p212.exists()) p212 else p
      case "2.12" => if (p212.exists()) p212 else p
      case _      => p
    }
  }

  private val oracleFileCheck = Def.setting { () =>
    // The test name is must match the expectations of problems.txt (only, not -2.11/-2.12.txt)
    val emptyProblemsTxt = IO.readLines(baseDirectory.value / "problems.txt").forall(_.startsWith("#"))
    name.value.takeRight(4).dropWhile(_ != '-') match {
      case "-ok"  => if (!emptyProblemsTxt) sys.error(s"[${name.value}] OK test with non-empty problems.txt")
      case "-nok" => if (emptyProblemsTxt) sys.error(s"[${name.value}] NOK test with empty problems.txt")
      case _      => sys.error(s"[${name.value}] Missing '-ok' or '-nok' suffix in project name")
    }
  }

  private val runIntegrationTest = Def.taskDyn {
    val confFile = baseDirectory.value / "test.conf"
    val conf = ConfigFactory.parseFile(confFile).resolve()
    val moduleBase = conf.getString("groupId") % conf.getString("artifactId")
    val depRes = dependencyResolution.value
    val v1 = getArtifact(depRes, moduleBase % conf.getString(V1.name), streams.value.log)
    val v2 = getArtifact(depRes, moduleBase % conf.getString(V2.name), streams.value.log)
    streams.value.log.info(s"Comparing $v1 -> $v2")
    runCollectProblemsTest(v1, v2)
  }

  private val funTestPerConfigSettings = Def.settings(
    Defaults.configSettings, // e.g. compile and package
    scalaSource := baseDirectory.value / configuration.value.name, // e.g., use v1/ instead of src/v1/scala/
  )

  private val testCollectProblemsImpl = Def.taskDyn {
    val v1 = (classDirectory in V1).toTask.dependsOn(compile in V1).value // compile the V1 sources and get the classes directory
    val v2 = (classDirectory in V2).toTask.dependsOn(compile in V2).value // same for V2
    runCollectProblemsTest(v1, v2)
  }

  private val testAppRunImpl = Def.task {
    val p    = baseDirectory.value / "testAppRun.pending"
    val p211 = baseDirectory.value / "testAppRun-2.11.pending"
    val p212 = baseDirectory.value / "testAppRun-2.12.pending"
    val pending = scalaVersion.value.take(4) match {
      case "2.11" => p211.exists() || p212.exists() || p.exists()
      case "2.12" => p212.exists() || p.exists()
      case _      => p.exists()
    }
    (App / fgRun).toTask("").value
    val result = (Test / fgRun).toTask("").result.value
    if (IO.read(oracleFile.value).isEmpty) {
      if (!pending) Result.tryValue(result)
    } else {
      if (!pending) result.toEither.foreach { (_: Unit) =>
        throw new MessageOnlyException(s"Test '${name.value}' failed: expected running App to fail")
      }
    }
  }

  private val runFunctionalTest = Def.task {
    testCollectProblems.value
    testAppRun.value
    streams.value.log.info(s"Test '${name.value}' succeeded")
  }

  private val funTestProjectSettings = Def.settings(
    scalaVersion := testScalaVersion.value,
    inConfig(V1)(funTestPerConfigSettings),
    inConfig(V2)(funTestPerConfigSettings),
    inConfig(App)(Def.settings(
      funTestPerConfigSettings,
      run / trapExit := false,
    )),
    inConfig(Test)(Def.settings(
      internalDependencyClasspath ++= (V2 / exportedProducts).value,
      internalDependencyClasspath ++= (App / exportedProducts).value,
      run / mainClass := (App / run / mainClass).value,
      run / trapExit  := false,
    )),
    Global / onLoad += oracleFileCheck.value,
    testCollectProblems := testCollectProblemsImpl.value,
    testAppRun := testAppRunImpl.value,
    Test / test := runFunctionalTest.value,
  )

  private def runCollectProblemsTest(oldJarOrDir: File, newJarOrDir: File) = Def.task {
    val cp = (functionalTests / Compile / fullClasspath).value // the test classpath from the functionalTest project for the test
    val si = (functionalTests / scalaInstance).value // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm

    val loader = new URLClassLoader(Attributed.data(cp).map(_.toURI.toURL).toArray, si.loader)

    val testClass = loader.loadClass("com.typesafe.tools.mima.lib.CollectProblemsTest$")
    val testRunner = testClass.getField("MODULE$").get(null).asInstanceOf[{
      def runTest(
          testClasspath: Array[File],
          testName: String,
          oldJarOrDir: File,
          newJarOrDir: File,
          baseDir: File,
          oracleFile: File,
      ): Unit
    }]

    // Add the scala-library to the MiMa classpath used to run this test
    val testClasspath = Attributed.data(cp).filter(_.getName.endsWith("scala-library.jar")).toArray

    try {
      import scala.language.reflectiveCalls
      testRunner.runTest(testClasspath, name.value, oldJarOrDir, newJarOrDir, baseDirectory.value, oracleFile.value)
      streams.value.log.info(s"Test '${name.value}' succeeded.")
    } catch {
      case e: Exception =>
        Console.err.println(e.toString)
        throw new MessageOnlyException(s"Test '${name.value}' failed")
    }
  }

  private def getArtifact(depResolver: DependencyResolution, m: ModuleID, log: Logger): File = {
    val md = depResolver.wrapDependencyInModule(m)
    val updateConf = UpdateConfiguration().withLogging(UpdateLogging.DownloadOnly)
    depResolver.update(md, updateConf, UnresolvedWarningConfiguration(), log) match {
      case Left(unresolvedWarning) =>
        import ShowLines._
        unresolvedWarning.lines.foreach(log.warn(_))
        throw unresolvedWarning.resolveException
      case Right(updateReport) =>
        val allFiles = for {
          conf <- updateReport.configurations
          module <- conf.modules
          (artifact, file) <- module.artifacts
          if artifact.name == m.name
        } yield file

        allFiles.headOption.getOrElse(sys.error(s"Could not resolve artifact: $m"))
    }
  }

  private def dependOnAll(projects: Seq[Project], task: TaskKey[Unit]): Def.Initialize[Task[Unit]] =
    Def.taskDyn {
      val structure = Project.structure(state.value)
      val allTasks = projects.flatMap(p => (p / task).get(structure.data))
      Def.task(allTasks.join.map(_ => ()).value)
    }

  private def dirContaining(oracleFilename: String): FileFilter = {
    DirectoryFilter && new SimpleFileFilter(_.list.contains(oracleFilename))
  }
}
