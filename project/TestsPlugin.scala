package mimabuild

import java.net.URLClassLoader
import bintray.BintrayPlugin
import com.typesafe.config.ConfigFactory
import sbt._
import sbt.Keys._
import sbt.internal.inc.ScalaInstance
import sbt.librarymanagement.{ DependencyResolution, UnresolvedWarningConfiguration, UpdateConfiguration }
import scala.Console

object TestsPlugin extends AutoPlugin {
  object autoImport {
    // This is the key for the scala version used to compile the tests, so that we can cross test the MiMa version
    // actually being used in the sbt plugin against multiple scala compiler versions.
    // Also the base project has dependencies that don't resolve under newer versions of scala.
    val testScalaVersion = settingKey[String]("The scala version to use to compile the test classes")

    val testFunctional = taskKey[Unit]("Run the functional test")
  }
  import autoImport._

  override def extraProjects = funTestProjects ++ intTestProjects

  override def buildSettings = Seq(
    testScalaVersion := sys.props.getOrElse("mima.testScalaVersion", scalaVersion.value),
  )

  override def projectSettings = Seq(
            testFunctional := dependOnAll(funTestProjects, _ /            Test / test).value,
    IntegrationTest / test := dependOnAll(intTestProjects, _ / IntegrationTest / test).value,
  )

  private val functionalTests = LocalProject("functional-tests")

  private val V1 = config("v1").extend(Compile)
  private val V2 = config("v2").extend(Compile)

  private def testProjects(prefix: String, fileName: String, setup: Project => Project) = {
    (file("functional-tests") / "src" / prefix * dirContaining(fileName)).get().map { base =>
      Project(s"$prefix-${base.name}", base).disablePlugins(BintrayPlugin).configure(setup)
    }
  }

  private def intTestProject(p: Project) = p.settings(intTestProjectSettings)
  private def funTestProject(p: Project) = p.settings(funTestProjectSettings).configs(V1, V2)

  private lazy val funTestProjects = testProjects("test", "problems.txt", funTestProject)
  private lazy val intTestProjects = testProjects( "it" ,   "test.conf" , intTestProject)

  private def sharedTestProjectSettings = Def.settings(
    resolvers += "scala-pr-validation-snapshots" at "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/",
    scalaVersion := testScalaVersion.value,
  )

  private val runIntegrationTest = Def.task {
    val cp = (functionalTests / Compile / fullClasspath).value // the test classpath from the functionalTest project for the test
    val si = (functionalTests / scalaInstance).value // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
    val confFile = baseDirectory.value / "test.conf"
    val conf = ConfigFactory.parseFile(confFile).resolve()
    val moduleBase = conf.getString("groupId") % conf.getString("artifactId")
    val depRes = dependencyResolution.value
    val v1 = getArtifact(depRes, moduleBase % conf.getString("v1"), streams.value.log)
    val v2 = getArtifact(depRes, moduleBase % conf.getString("v2"), streams.value.log)
    streams.value.log.info(s"Comparing $v1 -> $v2")
    runCollectProblemsTest(cp, si, streams.value, name.value, v1, v2, baseDirectory.value, scalaVersion.value)
    streams.value.log.info(s"Test '${name.value}' succeeded.")
  }

  private val intTestProjectSettings = Def.settings(
    sharedTestProjectSettings,
    IntegrationTest / test := runIntegrationTest.value,
  )

  private val funTestPerConfigSettings = Def.settings(
    Defaults.configSettings, // e.g. compile and package
    scalaSource := baseDirectory.value / configuration.value.name, // e.g., use v1/ instead of src/v1/scala/
  )

  private val runFunctionalTest = Def.task {
    val cp = (functionalTests / Compile / fullClasspath).value // the test classpath from the functionalTest project for the test
    val si = (functionalTests / scalaInstance).value // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
    (V1 / compile).value: Unit
    (V2 / compile).value: Unit
    val v1 = (V1 / classDirectory).value // compile the V1 sources and get the classes directory
    val v2 = (V2 / classDirectory).value // same for V2
    runCollectProblemsTest(cp, si, streams.value, name.value, v1, v2, baseDirectory.value, scalaVersion.value)
  }

  private val funTestProjectSettings = Def.settings(
    sharedTestProjectSettings,
    inConfig(V1)(funTestPerConfigSettings),
    inConfig(V2)(funTestPerConfigSettings),
    Test / test := runFunctionalTest.value,
  )

  private def runCollectProblemsTest(
      cp: Classpath,
      si: ScalaInstance,
      streams: TaskStreams,
      testName: String,
      oldJarOrDir: File,
      newJarOrDir: File,
      projectPath: File,
      scalaVersion: String,
  ): Unit = {
    val loader = new URLClassLoader(Attributed.data(cp).map(_.toURI.toURL).toArray, si.loader)

    val testClass = loader.loadClass("com.typesafe.tools.mima.lib.CollectProblemsTest$")
    val testRunner = testClass.getField("MODULE$").get(null).asInstanceOf[{
      def runTest(
          testClasspath: Array[File],
          testName: String,
          oldJarOrDir: File,
          newJarOrDir: File,
          baseDir: File,
          scalaVersion: String,
      ): Unit
    }]

    // Add the scala-library to the MiMa classpath used to run this test
    val testClasspath = Attributed.data(cp).filter(_.getName.endsWith("scala-library.jar")).toArray

    try {
      import scala.language.reflectiveCalls
      testRunner.runTest(testClasspath, testName, oldJarOrDir, newJarOrDir, projectPath, scalaVersion)
      streams.log.info(s"Test '$testName' succeeded.")
    } catch {
      case e: Exception =>
        Console.err.println(e.toString)
        throw new MessageOnlyException(s"Test '$testName' failed.")
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

  private def dependOnAll(projects: Seq[Project], f: Project => TaskKey[Unit]): Def.Initialize[Task[Unit]] =
    Def.taskDyn {
      val structure = Project.structure(state.value)
      val allTasks = projects.flatMap(p => f(p).get(structure.data))
      Def.task(allTasks.join.map(_ => ()).value)
    }

  private def dirContaining(oracleFilename: String): FileFilter = {
    DirectoryFilter && new SimpleFileFilter(_.list.contains(oracleFilename))
  }
}
