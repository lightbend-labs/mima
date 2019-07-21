package mimabuild

import bintray.BintrayPlugin
import com.typesafe.config.ConfigFactory
import sbt._
import sbt.Keys._
import sbt.internal.inc.ScalaInstance
import sbt.librarymanagement.{ DependencyResolution, UnresolvedWarningConfiguration, UpdateConfiguration }

object TestsPlugin extends AutoPlugin {
  object autoImport {
    // This is the key for the scala version used to compile the tests, so that we can cross test the MiMa version
    // actually being used in the sbt plugin against multiple scala compiler versions.
    // Also the base project has dependencies that don't resolve under newer versions of scala.
    val testScalaVersion = settingKey[String]("The scala version to use to compile the test classes")

    val testFunctional = taskKey[Unit]("Run the functional test")
  }
  import autoImport._

  override def extraProjects = tests ++ integrationTests

  override def buildSettings = Seq(
    testScalaVersion := sys.props.getOrElse("mima.testScalaVersion", scalaVersion.value),
  )

  override def projectSettings = Seq(
            testFunctional := dependOnAll(_ /                   test).value,
    IntegrationTest / test := dependOnAll(_ / IntegrationTest / test).value,
  )

  private val functionalTests = LocalProject("functional-tests")

  // define configurations for the V1 and V2 sources
  private val V1 = config("V1").extend(Compile)
  private val V2 = config("V2").extend(Compile)

  // select all testN directories.
  private val bases                = file("functional-tests") / "src" / "test" * dirContaining("problems.txt")
  private val integrationTestBases = file("functional-tests") / "src" / "it" * dirContaining("test.conf")

  // make the Project for each discovered directory
  private lazy val tests            = bases.get.map(testProject)
  private lazy val integrationTests = integrationTestBases.get.map(integrationTestProject)

  private def integrationTestProject(base: File) =
    Project(s"it-${base.name}", base).disablePlugins(BintrayPlugin).settings(integrationTestProjectSettings)

  private val runIntegrationTest = Def.task {
    val confFile = baseDirectory.value / "test.conf"
    val conf = ConfigFactory.parseFile(confFile).resolve()
    val moduleBase = conf.getString("groupId") % conf.getString("artifactId")
    val depRes = dependencyResolution.value
    val oldJar = getArtifact(depRes, moduleBase % conf.getString("v1"), streams.value.log)
    val newJar = getArtifact(depRes, moduleBase % conf.getString("v2"), streams.value.log)
    streams.value.log.info(s"Comparing $oldJar -> $newJar")
    runCollectProblemsTest(
      (functionalTests / Compile / fullClasspath).value, // the test classpath from the functionalTest project for the test
      (functionalTests / scalaInstance).value, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      streams.value,
      thisProjectRef.value.project,
      oldJar,
      newJar,
      baseDirectory.value,
      scalaVersion.value,
      confFile.getAbsolutePath)
  }

  private val integrationTestProjectSettings = Def.settings(
    scalaVersion := testScalaVersion.value,
    crossScalaVersions := Seq("2.12.8", "2.13.0"),
    IntegrationTest / test := runIntegrationTest.value,
  )

  // defines a Project for the given base directory (for example, functional-tests/test1)
  // Its name is the directory name (test1) and it has compile+package tasks for sources in v1/ and v2/
  private def testProject(base: File) =
    Project(s"test-${base.name}", base).disablePlugins(BintrayPlugin).settings(testProjectSettings).configs(V1, V2)

  // The settings for the V1 and V2 configurations (e.g. add compile and package).
  private val testPerConfigSettings = Def.settings(
    Defaults.configSettings, // use the normal per-configuration settings
    // but modify the source directory to be just V1/ instead of src/V1/scala/
    // scalaSource is the setting key that defines the directory for Scala sources
    // configuration gets the current configuration
    scalaSource := baseDirectory.value / configuration.value.name.toLowerCase,
  )

  private val runFunctionalTest = Def.task {
    runCollectProblemsTest(
      (functionalTests / Compile / fullClasspath).value, // the test classpath from the functionalTest project for the test
      (functionalTests / scalaInstance).value, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      streams.value,
      thisProjectRef.value.project,
      (V1 / packageBin).value, // package the V1 sources and get the configuration used
      (V2 / packageBin).value, // same for V2
      baseDirectory.value,
      scalaVersion.value,
      null,
    )
  }

  private val testProjectSettings = Def.settings(
    resolvers += "scala-pr-validation-snapshots" at "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/",
    scalaVersion := testScalaVersion.value,
    crossScalaVersions := Seq("2.12.8", "2.13.0"),
    inConfig(V1)(testPerConfigSettings),
    inConfig(V2)(testPerConfigSettings),
    test := runFunctionalTest.value,
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
      filterPath: String,
  ): Unit = {
    val urls = Attributed.data(cp).map(_.toURI.toURL).toArray
    val loader = new java.net.URLClassLoader(urls, si.loader)

    val testClass = loader.loadClass("com.typesafe.tools.mima.lib.CollectProblemsTest")
    val testRunner = testClass.getDeclaredConstructor().newInstance().asInstanceOf[ {
      def runTest(testClasspath: Array[String], testName: String, oldJarPath: String,
          newJarPath: String, oraclePath: String, filterPath: String): Unit
    }]

    // Add the scala-library to the MiMa classpath used to run this test
    val testClasspath = Attributed.data(cp).filter(_.getName endsWith "scala-library.jar")
        .map(_.getAbsolutePath).toArray

    val oracleFile = {
      val p = projectPath / "problems.txt"
      val p212 = projectPath / "problems-2.12.txt"
      val p213 = projectPath / "problems-2.13.txt"
      scalaVersion.take(4) match {
        case "2.13" if p213.exists() => p213
        case "2.13" if p212.exists() => p212
        case "2.12" if p212.exists() => p212
        case _ => p
      }
    }

    try {
      import scala.language.reflectiveCalls
      testRunner.runTest(testClasspath, testName, oldJarOrDir.getAbsolutePath, newJarOrDir.getAbsolutePath,
        oracleFile.getAbsolutePath, filterPath)
      streams.log.info(s"Test '$testName' succeeded.")
    } catch {
      case e: Exception => sys.error(e.toString)
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

        allFiles.headOption getOrElse sys.error(s"Could not resolve artifact: $m")
    }
  }

  private def dependOnAll(f: ProjectRef => TaskKey[Unit]): Def.Initialize[Task[Unit]] = Def.taskDyn {
    val proj = thisProjectRef.value
    val structure = Project.structure(state.value)
    val allProjects = structure.allProjectRefs(proj.build).filter(_ != proj) // exclude self
    val allTasks = allProjects.flatMap(p => f(p).get(structure.data))
    Def.task(allTasks.join.map(_ => ()).value)
  }

  private def dirContaining(oracleFilename: String): FileFilter = {
    DirectoryFilter && new SimpleFileFilter(_.list.contains(oracleFilename))
  }
}
