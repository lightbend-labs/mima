import sbt._
import Keys._
import com.typesafe.config.ConfigFactory
import sbt.internal.inc.ScalaInstance
import sbt.librarymanagement.{ DependencyResolution, UnresolvedWarningConfiguration, UpdateConfiguration }
import sbt.plugins.SbtPlugin

import bintray.BintrayPlugin
import bintray.BintrayPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitPlugin.autoImport._
import sbt.ScriptedPlugin.autoImport._
import com.typesafe.sbt.SbtPgp.autoImport.PgpKeys.publishSigned
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

object BuildSettings {

  val buildName = "mima"
  val buildOrganization = "com.typesafe"

  val commonSettings = Seq(
      organization := buildOrganization,
      scalaVersion := sys.props.getOrElse("mima.buildScalaVersion", "2.12.8"),
      git.gitTagToVersionNumber := { tag: String =>
        if (tag matches "[0.9]+\\..*") Some(tag)
        else None
      },
      git.useGitDescribe := true,
      licenses := Seq("Apache License v2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      homepage := Some(url("http://github.com/lightbend/migration-manager")),
      scalacOptions := Seq("-feature", "-deprecation", "-Xlint", "-Xfuture")
  )

  val mimaSettings = Def settings (
    mimaPreviousArtifacts := Set({
      val m = organization.value % moduleName.value % "0.1.15"
      if (sbtPlugin.value)
        Defaults.sbtPluginExtra(m, (sbtBinaryVersion in pluginCrossBuild).value, (scalaBinaryVersion in update).value)
      else
        m cross CrossVersion.binary
    })
  )

  def sbtPublishSettings: Seq[Def.Setting[_]] = Def settings (
    mimaSettings,
    bintrayOrganization := Some("typesafe"),
    bintrayRepository := "sbt-plugins",
    bintrayReleaseOnPublish := false
  )

  def sonatypePublishSettings: Seq[Def.Setting[_]] = Def settings (
    mimaSettings,
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
    pomIncludeRepository := { _ => false },
    // Maven central wants some extra metadata to keep things 'clean'.
    pomExtra := (
      <scm>
        <url>git@github.com:lightbend/migration-manager.git</url>
        <connection>scm:git:git@github.com:lightbend/migration-manager.git</connection>
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
  val typesafeConfig = "com.typesafe" % "config" % "1.3.4"
  val scalatest = "org.scalatest" %% "scalatest" % "3.1.0-SNAP11" % Test
}

object MimaBuild {
  import BuildSettings._
  import Dependencies._

  lazy val root = (
    project("root", file("."))
    aggregate(core, reporter, sbtplugin)
    settings(name := buildName,
             publish := {},
             publishLocal := {},
             publishSigned := {},
             testScalaVersion in Global :=  sys.props.getOrElse("mima.testScalaVersion", scalaVersion.value)
    )
    enablePlugins(GitVersioning)
  )

  val scalaPartV = Def setting (CrossVersion partialVersion scalaVersion.value)

  lazy val core = (
    project("core", file("core"))
    settings(
      commonSettings,
      name := buildName + "-core",
      libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      libraryDependencies += scalatest,
      // WORKAROUND https://github.com/sbt/sbt/issues/2819
      inConfig(Compile)(
        unmanagedSourceDirectories ++=
          scalaPartV.value.collect { case (2, 13) => sourceDirectory.value / "scala-2.13" }.toList
      )
    )
    settings(sonatypePublishSettings:_*)
    settings(
      mimaBinaryIssueFilters ++= {
        import com.typesafe.tools.mima.core._
        Seq(
          // Removed because unused
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.core.buildinfo.BuildInfo"),
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.core.buildinfo.BuildInfo$"),

          // Add support for versions with less segments (#212)
          ProblemFilters.exclude[ReversedMissingMethodProblem]("com.typesafe.tools.mima.core.util.log.Logging.warn"),
          ProblemFilters.exclude[ReversedMissingMethodProblem]("com.typesafe.tools.mima.core.util.log.Logging.error")
        )
      }
    )
  )

  lazy val reporter = (
    project("reporter", file("reporter"))
    dependsOn(core)
    settings(
      commonSettings,
      libraryDependencies += typesafeConfig,
      name := buildName + "-reporter",
      sonatypePublishSettings,
      // add task testFunctional that depends on all the functional tests
      testFunctional := allTests(testFunctional in _).value,
      test in IntegrationTest := allTests(test in IntegrationTest in _).value,

      mimaBinaryIssueFilters ++= {
        import com.typesafe.tools.mima.core._
        Seq(
          // Removed because unused
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.lib.license.License"),
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.lib.license.License$"),

          // Removed because CLI dropped
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.MimaSpec"),
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.MimaSpec$"),
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.Main"),
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.Main$"),

          // Moved (to functional-tests) because otherwise unused
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.ProblemFiltersConfig"),
          ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.ProblemFiltersConfig$")
        )
      }
    )
  )

  lazy val sbtplugin = (
    Project("sbtplugin", file("sbtplugin"))
    enablePlugins(SbtPlugin)
    settings(name := "sbt-mima-plugin",
             commonSettings,
             libraryDependencies += scalatest,
             scriptedLaunchOpts += "-Dplugin.version=" + version.value,
             // Scripted locally publishes sbt plugin and then runs test projects with locally published version.
             // Therefore we also need to locally publish dependent projects on scripted test run.
             scriptedDependencies := scriptedDependencies.dependsOn(publishLocal in core, publishLocal in reporter).value,
    )
    dependsOn(reporter)
    settings(
      sbtPublishSettings,
      mimaBinaryIssueFilters ++= {
        import com.typesafe.tools.mima.core._
        Seq(
          // sbt-compat has been created to define these and has been added as a dependency of sbt-mima-plugin
          ProblemFilters.exclude[MissingClassProblem]("sbt.compat"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.compat$"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.package"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.package$"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.package$UpdateConfigurationOps"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.package$UpdateConfigurationOps$"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.UpdateConfiguration"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.UpdateConfiguration$"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.DependencyResolution"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.ivy.IvyDependencyResolution"),
          ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.ivy.IvyDependencyResolution$"),

          // Add support for versions with less segments (#212)
          // All of these are MiMa sbtplugin internals and can be safely filtered
          ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.typesafe.tools.mima.plugin.SbtMima.runMima"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("com.typesafe.tools.mima.plugin.SbtMima.reportErrors"),
          ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.typesafe.tools.mima.plugin.SbtMima.reportModuleErrors")
        )
      }
    )
  )

  lazy val functionalTests = project("functional-tests", file("functional-tests"))
    .dependsOn(core, reporter)
    .settings(commonSettings)

  // select all testN directories.
  val bases = (file("functional-tests") / "src" / "test") *
    (DirectoryFilter && new SimpleFileFilter(_.list.contains("problems.txt")))

  val integrationTestBases = (file("functional-tests") / "src" / "it") *
    (DirectoryFilter && new SimpleFileFilter(_.list.contains("test.conf")))

  // make the Project for each discovered directory
  lazy val tests = bases.get map testProject
  lazy val integrationTests = integrationTestBases.get map integrationTestProject

  // defines a Project for the given base directory (for example, functional-tests/test1)
  // Its name is the directory name (test1) and it has compile+package tasks for sources in v1/ and v2/
  def testProject(base: File) = project("test-" + base.name, base).settings(testProjectSettings).configs(V1, V2)
  def integrationTestProject(base: File) = project("it-" + base.name, base).settings(integrationTestProjectSettings)

  lazy val testProjectSettings =
    commonSettings ++ // normal project defaults; can be trimmed later- test and run aren't needed, for example.
    Seq(scalaVersion := (testScalaVersion in Global).value) ++
    inConfig(V1)(perConfig) ++ // add compile/package for the V1 sources
    inConfig(V2)(perConfig) :+ // add compile/package for the V2 sources
    (testFunctional := runTest.value) // add the testFunctional task
  lazy val integrationTestProjectSettings =
    commonSettings ++
    Seq(scalaVersion := (testScalaVersion in Global).value) ++
    (test in IntegrationTest := runIntegrationTest.value)

  val testFunctional = taskKey[Unit]("Run the functional test")

  // This is the key for the scala version used to compile the tests, so that we can cross test the MiMa version
  // actually being used in the sbt plugin against multiple scala compiler versions.
  // Also the base project has dependencies that don't resolve under newer versions of scala.
  val testScalaVersion = settingKey[String]("The scala version to use to compile the test classes")

  // define configurations for the V1 and V2 sources
  val V1 = config("V1") extend Compile
  val V2 = config("V2") extend Compile

  // these are settings defined for each configuration (V1 and V2).
  // We use the normal per-configuration settings, but modify the source directory to be just V1/ instead of src/V1/scala/
  lazy val perConfig = Defaults.configSettings :+ shortSourceDir

  // sets the source directory in this configuration to be: testN / vN
  // scalaSource is the setting key that defines the directory for Scala sources
  // configuration gets the current configuration
  lazy val shortSourceDir = scalaSource := baseDirectory.value / configuration.value.name.toLowerCase

  lazy val runTest = Def.task {
    val proj = thisProjectRef.value // gives us the ProjectRef this task is defined in
    runCollectProblemsTest(
      (fullClasspath in (functionalTests, Compile)).value, // the test classpath from the functionalTest project for the test
      (scalaInstance in core).value, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      streams.value,
      proj.project,
      (packageBin in V1).value, // package the V1 sources and get the configuration used
      (packageBin in V2).value, // same for V2
      baseDirectory.value,
      scalaVersion.value,
      null)
  }

  lazy val runIntegrationTest = Def.task {
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
      (scalaInstance in core).value, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      streams.value,
      proj.project,
      jar1,
      jar2,
      baseDirectory.value,
      scalaVersion.value,
      confFile.getAbsolutePath)
  }

  def runCollectProblemsTest(
      cp: Keys.Classpath,
      si: ScalaInstance,
      streams: Keys.TaskStreams,
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
    val testRunner = testClass.newInstance().asInstanceOf[ {
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
    Project(id, base).settings(settings).disablePlugins(BintrayPlugin)
}

object DefineTestProjectsPlugin extends AutoPlugin {
  override def extraProjects = MimaBuild.tests ++ MimaBuild.integrationTests
}
