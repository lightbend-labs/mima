package mimabuild

import bintray.BintrayPlugin
import com.typesafe.config.ConfigFactory
import sbt._
import sbt.Keys._
import sbt.internal.inc.ScalaInstance
import sbt.librarymanagement.{ DependencyResolution, UnresolvedWarningConfiguration, UpdateConfiguration }

object TestsPlugin extends AutoPlugin {
  override def extraProjects = tests ++ integrationTests

  object autoImport {
    // This is the key for the scala version used to compile the tests, so that we can cross test the MiMa version
    // actually being used in the sbt plugin against multiple scala compiler versions.
    // Also the base project has dependencies that don't resolve under newer versions of scala.
    val testScalaVersion = settingKey[String]("The scala version to use to compile the test classes")

    val testFunctional = taskKey[Unit]("Run the functional test")
  }
  import autoImport._

  override def buildSettings = Seq(
    testScalaVersion := sys.props.getOrElse("mima.testScalaVersion", scalaVersion.value),
  )

  override def projectSettings = Seq(
    testFunctional := dependOnAll(test in _).value,
    test in IntegrationTest := dependOnAll(test in IntegrationTest in _).value,
  )

  val functionalTests = LocalProject("functional-tests")

  // define configurations for the V1 and V2 sources
  val V1 = config("V1") extend Compile
  val V2 = config("V2") extend Compile

  // select all testN directories.
  val bases = (file("functional-tests") / "src" / "test") *
    (DirectoryFilter && new SimpleFileFilter(_.list.contains("problems.txt")))

  val integrationTestBases = (file("functional-tests") / "src" / "it") *
    (DirectoryFilter && new SimpleFileFilter(_.list.contains("test.conf")))

  // make the Project for each discovered directory
  lazy val tests            = bases.get map testProject
  lazy val integrationTests = integrationTestBases.get map integrationTestProject

  def integrationTestProject(base: File) =
    Project("it-" + base.name, base).disablePlugins(BintrayPlugin).settings(integrationTestProjectSettings)

  val runIntegrationTest = Def.task {
    val proj = thisProjectRef.value // gives us the ProjectRef this task is defined in
    val confFile = baseDirectory.value / "test.conf"
    val conf = ConfigFactory.parseFile(confFile).resolve()
    val moduleBase = conf.getString("groupId") % conf.getString("artifactId")
    val depRes: DependencyResolution = dependencyResolution.value
    val jar1 = getArtifact(depRes, moduleBase % conf.getString("v1"), streams.value)
    val jar2 = getArtifact(depRes, moduleBase % conf.getString("v2"), streams.value)
    streams.value.log.info(s"Comparing $jar1 -> $jar2")
    runCollectProblemsTest(
      (fullClasspath in (functionalTests, Compile)).value, // the test classpath from the functionalTest project for the test
      (scalaInstance in functionalTests).value, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      streams.value,
      proj.project,
      jar1,
      jar2,
      baseDirectory.value,
      scalaVersion.value,
      confFile.getAbsolutePath)
  }

  val integrationTestProjectSettings = Def.settings(
    scalaVersion := testScalaVersion.value,
    crossScalaVersions := Seq("2.12.8", "2.13.0"),
    test in IntegrationTest := runIntegrationTest.value,
  )

  // defines a Project for the given base directory (for example, functional-tests/test1)
  // Its name is the directory name (test1) and it has compile+package tasks for sources in v1/ and v2/
  def testProject(base: File) =
    Project("test-" + base.name, base).disablePlugins(BintrayPlugin).settings(testProjectSettings).configs(V1, V2)

  // sets the source directory in this configuration to be: testN / vN
  // scalaSource is the setting key that defines the directory for Scala sources
  // configuration gets the current configuration
  val shortSourceDir = scalaSource := baseDirectory.value / configuration.value.name.toLowerCase

  // these are settings defined for each configuration (V1 and V2).
  // We use the normal per-configuration settings, but modify the source directory to be just V1/ instead of src/V1/scala/
  val perConfig = Defaults.configSettings :+ shortSourceDir

  val runTest = Def.task {
    val proj = thisProjectRef.value // gives us the ProjectRef this task is defined in
    runCollectProblemsTest(
      (fullClasspath in (functionalTests, Compile)).value, // the test classpath from the functionalTest project for the test
      (scalaInstance in functionalTests).value, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      streams.value,
      proj.project,
      (packageBin in V1).value, // package the V1 sources and get the configuration used
      (packageBin in V2).value, // same for V2
      baseDirectory.value,
      scalaVersion.value,
      null,
    )
  }

  val testProjectSettings = Def.settings(
    resolvers += "scala-pr-validation-snapshots" at "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/",
    scalaVersion := testScalaVersion.value,
    crossScalaVersions := Seq("2.12.8", "2.13.0"),
    inConfig(V1)(perConfig), // add compile/package for the V1 sources
    inConfig(V2)(perConfig), // add compile/package for the V2 sources
    test := runTest.value,
  )

  def runCollectProblemsTest(
      cp: Classpath,
      si: ScalaInstance,
      streams: TaskStreams,
      testName: String,
      v1: File,
      v2: File,
      projectPath: File,
      scalaV: String,
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
      scalaV.take(4) match {
        case "2.13" if p213.exists() => p213
        case "2.13" if p212.exists() => p212
        case "2.12" if p212.exists() => p212
        case _ => p
      }
    }

    try {
      import scala.language.reflectiveCalls
      testRunner.runTest(testClasspath, testName, v1.getAbsolutePath, v2.getAbsolutePath,
        oracleFile.getAbsolutePath, filterPath)
      streams.log.info("Test '" + testName + "' succeeded.")
    } catch {
      case e: Exception => sys.error(e.toString)
    }
  }

  def getArtifact(depResolver: DependencyResolution, m: ModuleID, s: TaskStreams): File = {
    val md = depResolver.wrapDependencyInModule(m)
    val updateConf = UpdateConfiguration().withLogging(UpdateLogging.DownloadOnly)
    depResolver.update(md, updateConf, UnresolvedWarningConfiguration(), s.log) match {
      case Left(unresolvedWarning) =>
        import sbt.util.ShowLines._
        unresolvedWarning.lines.foreach(s.log.warn(_))
        throw unresolvedWarning.resolveException
      case Right(updateReport) =>
        val allFiles =
          for {
            conf <- updateReport.configurations
            module <- conf.modules
            (artifact, file) <- module.artifacts
            if artifact.name == m.name
          } yield file

        allFiles.headOption getOrElse sys.error(s"Could not resolve artifact: $m")
    }
  }

  def dependOnAll(f: ProjectRef => TaskKey[Unit]): Def.Initialize[Task[Unit]] = Def.taskDyn {
    val proj = thisProjectRef.value
    val structure = Project.structure(state.value)
    val allProjects = structure.allProjectRefs(proj.build).filter(_ != proj) // exclude self
    val allTasks = allProjects.flatMap(p => f(p).get(structure.data))
    Def.task(allTasks.join.map(_ => ()).value)
  }
}
