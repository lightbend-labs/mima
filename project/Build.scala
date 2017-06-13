import sbt._
import Keys._
import com.typesafe.config.ConfigFactory

// I need to make these imported by default
import Project.inConfig
import Configurations.config
import Build.data
import sbtassembly.Plugin.AssemblyKeys
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin.assemblySettings
import sbtassembly.Plugin.MergeStrategy
import sbtbuildinfo.Plugin._
import bintray.BintrayPlugin
import bintray.BintrayPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitPlugin.autoImport._
import ScriptedPlugin._

object BuildSettings {

  val buildName = "mima"
  val buildOrganization = "com.typesafe"

  val commonSettings = Defaults.coreDefaultSettings ++ Seq (
      organization := buildOrganization,
      scalaVersion := sys.props.getOrElse("mima.buildScalaVersion", "2.10.6"),
      git.gitTagToVersionNumber := { tag: String =>
        if(tag matches "[0.9]+\\..*") Some(tag)
        else None
      },
      git.useGitDescribe := true,
      licenses := Seq("Apache License v2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      homepage := Some(url("http://github.com/typesafehub/migration-manager")),
      scalacOptions := Seq("-feature", "-deprecation", "-Xlint")
  )

  def sbtPublishSettings: Seq[Def.Setting[_]] = Seq(
    bintrayOrganization := Some("typesafe"),
    bintrayRepository := "sbt-plugins",
    bintrayReleaseOnPublish := false
  )

  def sonatypePublishSettings: Seq[Def.Setting[_]] = Seq(
    // If we want on maven central, we need to be in maven style.
    publishMavenStyle := true,
    publishArtifact in Test := false,
    // The Nexus repo we're publishing to.
    publishTo := Some(
      if (isSnapshot.value) "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      else                  "releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    ),
    // Maven central cannot allow other repos.  We're ok here because the artifacts we
    // we use externally are *optional* dependencies.
    pomIncludeRepository := { x => false },
    // Maven central wants some extra metadata to keep things 'clean'.
    pomExtra := (
      <scm>
        <url>git@github.com:typesafehub/migration-manager.git</url>
        <connection>scm:git:git@github.com:typesafehub/migration-manager.git</connection>
      </scm>
      <developers>
        <developer>
          <id>mdotta</id>
          <name>Mirco Dotta</name>
        </developer>
        <developer>
          <id>jsuereth</id>
          <name>Josh Suereth</name>
          <url>http://jsuereth.com</url>
        </developer>
      </developers>)
  )
}

object Dependencies {
  import BuildSettings._

  val typesafeConfig = "com.typesafe" % "config" % "1.0.0"
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % Test

}

object MimaBuild {
  import BuildSettings._
  import Dependencies._

  lazy val root = (
    project("root", file("."))
    aggregate(core, reporter, sbtplugin)
    settings(name := buildName,
             publish := (),
             publishLocal := (),
             testScalaVersion in Global :=  sys.props.getOrElse("mima.testScalaVersion", scalaVersion.value)
    )
    enablePlugins(GitVersioning)
  )

  lazy val core = (
    project("core", file("core"),
            settings = ((commonSettings ++ buildInfoSettings): Seq[Setting[_]]) ++: Seq(
                sourceGenerators in Compile += buildInfo.taskValue,
                buildInfoKeys := Seq(version),
                buildInfoPackage := "com.typesafe.tools.mima.core.buildinfo",
                buildInfoObject  := "BuildInfo"
                )
           )
    settings(libraryDependencies ++= Seq(
               "org.scala-lang" % "scala-compiler" % scalaVersion.value,
               scalatest
             ),
             name := buildName + "-core")
    settings(sonatypePublishSettings:_*)
  )

  val myAssemblySettings: Seq[Setting[_]] = (assemblySettings: Seq[Setting[_]]) ++ Seq(
    mergeStrategy in assembly ~= (old => {
      case "LICENSE" => MergeStrategy.first
      case x         => old(x)
    }),
    AssemblyKeys.excludedFiles in assembly ~= (old =>
      // Hack to keep LICENSE files.
      { files: Seq[File] => old(files) filterNot (_.getName contains "LICENSE") }
    )
  )

  lazy val reporter = (
    project("reporter", file("reporter"), settings = commonSettings)
    settings(libraryDependencies += typesafeConfig,
             name := buildName + "-reporter")
    dependsOn(core)
    settings(sonatypePublishSettings:_*)
    settings(myAssemblySettings:_*)
    settings(
      // add task functional-tests that depends on all functional tests
      functionalTests := allTests(functionalTests in _).value,
      test in IntegrationTest := allTests(test in IntegrationTest in _).value,
      mainClass in assembly := Some("com.typesafe.tools.mima.cli.Main")
    )
  )

  lazy val sbtplugin = (
    Project("sbtplugin", file("sbtplugin"), settings = commonSettings)
    settings(scriptedSettings)
    settings(name := "sbt-mima-plugin",
             sbtPlugin := true,
             scriptedLaunchOpts := scriptedLaunchOpts.value :+ "-Dplugin.version=" + version.value,
             scriptedBufferLog := false,
             // Scripted locally publishes sbt plugin and then runs test projects with locally published version.
             // Therefore we also need to locally publish dependent projects on scripted test run.
             scripted := (scripted dependsOn (publishLocal in core, publishLocal in reporter)).evaluated)
    dependsOn(reporter)
    settings(sbtPublishSettings:_*)
  )

  lazy val reporterFunctionalTests = project("reporter-functional-tests",
  										file("reporter") / "functional-tests" ,
  										settings = commonSettings)
  										.dependsOn(core, reporter)

  // select all testN directories.
  val bases = (file("reporter") / "functional-tests" / "src" / "test") *
    (DirectoryFilter && new SimpleFileFilter(_.list.contains("problems.txt")))
  val integrationTestBases = (file("reporter") / "functional-tests" / "src" / "it") *
    (DirectoryFilter && new SimpleFileFilter(_.list.contains("test.conf")))

  // make the Project for each discovered directory
  lazy val tests = bases.get map testProject
  lazy val integrationTests = integrationTestBases.get map integrationTestProject

  // defines a Project for the given base directory (for example, functional-tests/test1)
  // Its name is the directory name (test1) and it has compile+package tasks for sources in v1/ and v2/
  def testProject(base: File) = project("test-" + base.name, base, settings = testProjectSettings).configs(v1Config, v2Config)
  def integrationTestProject(base: File) = project("it-" + base.name, base, settings = integrationTestProjectSettings)

  lazy val testProjectSettings =
    commonSettings ++ // normal project defaults; can be trimmed later- test and run aren't needed, for example.
    Seq(scalaVersion := (testScalaVersion in Global).value) ++
    inConfig(v1Config)(perConfig) ++ // add compile/package for the v1 sources
    inConfig(v2Config)(perConfig) :+ // add compile/package for the v2 sources
    (functionalTests := runTest.value) // add the functional-tests task
  lazy val integrationTestProjectSettings =
    commonSettings ++
    Seq(scalaVersion := (testScalaVersion in Global).value) ++
    (test in IntegrationTest := runIntegrationTest.value)

  // this is the key for the task that runs the reporter's functional tests
  lazy val functionalTests = TaskKey[Unit]("test-functional")

  // This is the key for the scala version used to compile the tests, so that we can cross test the MiMa version
  // actually being used in the sbt plugin against multiple scala compiler versions.
  // Also the base project has dependencies that don't resolve under newer versions of scala.
  lazy val testScalaVersion = SettingKey[String]("test-scala-version", "The scala version to use to compile the test classes")

  // define configurations for the v1 and v2 sources
  lazy val v1Config = config("v1") extend Compile
  lazy val v2Config = config("v2") extend Compile

  // these are settings defined for each configuration (v1 and v2).
  // We use the normal per-configuration settings, but modify the source directory to be just v1/ instead of src/v1/scala/
  lazy val perConfig = Defaults.configSettings :+ shortSourceDir

  // sets the source directory in this configuration to be: testN / vN
  // scalaSource is the setting key that defines the directory for Scala sources
  // configuration gets the current configuration
  lazy val shortSourceDir = scalaSource := baseDirectory.value / configuration.value.name

  lazy val runTest = Def.task {
    val proj = thisProjectRef.value // gives us the ProjectRef this task is defined in
    runCollectProblemsTest(
      (fullClasspath in (reporterFunctionalTests, Compile)).value, // the test classpath from the functionalTest project for the test
      (scalaInstance in core).value, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      streams.value,
      proj.project,
      (packageBin in v1Config).value, // package the v1 sources and get the configuration used
      (packageBin in v2Config).value, // same for v2
      baseDirectory.value,
      scalaVersion.value,
      null)
  }

  lazy val runIntegrationTest = Def.task {
    val proj = thisProjectRef.value // gives us the ProjectRef this task is defined in
    val confFile = baseDirectory.value / "test.conf"
    val conf = ConfigFactory.parseFile(confFile).resolve()
    val moduleBase = conf.getString("groupId") % conf.getString("artifactId")
    val jar1 = getArtifact(moduleBase % conf.getString("v1"), ivySbt.value, streams.value)
    val jar2 = getArtifact(moduleBase % conf.getString("v2"), ivySbt.value, streams.value)
    streams.value.log.info(s"Comparing $jar1 -> $jar2")
    runCollectProblemsTest(
      (fullClasspath in (reporterFunctionalTests, Compile)).value, // the test classpath from the functionalTest project for the test
      (scalaInstance in core).value, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      streams.value,
      proj.project,
      jar1,
      jar2,
      baseDirectory.value,
      scalaVersion.value,
      confFile.getAbsolutePath)
  }

  def runCollectProblemsTest(cp: Keys.Classpath, si: ScalaInstance, streams: Keys.TaskStreams, testName: String, v1: File, v2: File, projectPath: File, scalaV: String, filterPath: String): Unit = {
    val urls = Attributed.data(cp).map(_.toURI.toURL).toArray
    val loader = new java.net.URLClassLoader(urls, si.loader)

    val testClass = loader.loadClass("com.typesafe.tools.mima.lib.CollectProblemsTest")
    val testRunner = testClass.newInstance().asInstanceOf[{
      def runTest(testClasspath: Array[String], testName: String, oldJarPath: String, newJarPath: String, oraclePath: String, filterPath: String): Unit
    }]

    // Add the scala-library to the MiMa classpath used to run this test
    val testClasspath = Attributed.data(cp).filter(_.getName endsWith "scala-library.jar").map(_.getAbsolutePath).toArray

    val oracleFile = {
      val p = projectPath / "problems.txt"
      val p212 = projectPath / "problems-2.12.txt"
      if(!(scalaV.startsWith("2.10.") || scalaV.startsWith("2.11.")) && p212.exists) p212
      else p
    }

    try {
      import scala.language.reflectiveCalls
      testRunner.runTest(testClasspath, testName, v1.getAbsolutePath, v2.getAbsolutePath, oracleFile.getAbsolutePath, filterPath)
      streams.log.info("Test '" + testName + "' succeeded.")
    } catch {
      case e: Exception =>  sys.error(e.toString)
    }
  }

  def getArtifact(m: ModuleID, ivy: IvySbt, s: TaskStreams): File = {
    val moduleSettings = InlineConfiguration(
      "dummy" % "test" % "version",
      ModuleInfo("dummy-test-project-for-resolving"),
      dependencies = Seq(m))
    val module = new ivy.Module(moduleSettings)
    val report = Deprecated.Inner.ivyUpdate(ivy)(module, s)
    val optFile = (for {
      config <- report.configurations
      module <- config.modules
      (artifact, file) <- module.artifacts
      if artifact.name == m.name
    } yield file).headOption
    optFile getOrElse sys.error("Could not resolve artifact: " + m)
  }

  def allTests(f: ProjectRef => TaskKey[Unit]) = Def.taskDyn {
    val s = state.value // this is how we access all defined projects from a task
    val proj = thisProjectRef.value // gives us the ProjectRef this task is defined in
    val _ = (test in Test).value // requires unit tests to run first

    // gets all defined projects, dropping this project (core) so the task doesn't depend on itself
    val structure = Project.structure(s)
    val allProjects = structure.units(proj.build).defined.values filter (_.id != proj.project)

    // get the fun-tests task in each project
    val allTests = allProjects.toSeq flatMap { p => f(ProjectRef(proj.build, p.id)) get structure.data }

    // depend on all fun-tests
    Def.task {
      allTests.join.map(_ => ()).value
    }
  }

  def project(id: String, base: File, settings: Seq[Def.Setting[_]] = Nil) =
    Project(id, base, settings = settings) disablePlugins(BintrayPlugin)
}

object DefineTestProjectsPlugin extends AutoPlugin {
  override def extraProjects = MimaBuild.tests ++ MimaBuild.integrationTests
}

// use the SI-7934 workaround to silence a deprecation warning on an sbt API
// we have no choice but to call.  on the lack of any suitable alternative,
// see https://gitter.im/sbt/sbt-dev?at=5616e2681b0e279854bd74a4 :
// "it's my intention to eventually come up with a public API" says Eugene Y
object Deprecated {
  @deprecated("", "") class Inner {
    def ivyUpdate(ivy: IvySbt)(module: ivy.Module, s: TaskStreams) =
      IvyActions.update(
        module,
        new UpdateConfiguration(
          retrieve = None,
          missingOk = false,
          logging = UpdateLogging.DownloadOnly),
        s.log)
  }
  object Inner extends Inner
}
