import sbt._
import Keys._

// I need to make these imported by default
import Project.inConfig
import Configurations.config
import Build.data
import sbtassembly.Plugin.AssemblyKeys
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin.assemblySettings
import sbtassembly.Plugin.MergeStrategy
import sbtbuildinfo.Plugin._
import com.typesafe.sbt.S3Plugin._
import S3._
import bintray.BintrayPlugin
import bintray.BintrayPlugin.autoImport._
import com.typesafe.sbt.GitVersioning
import com.typesafe.sbt.GitPlugin.autoImport._

object BuildSettings {

  val buildName = "mima"
  val buildOrganization = "com.typesafe"

  val buildScalaVer = "2.10.6"

  val commonSettings = Defaults.defaultSettings ++ Seq (
      organization := buildOrganization,
      scalaVersion := buildScalaVer,
      git.baseVersion := "0.1",
      git.gitTagToVersionNumber := { tag: String =>
        if(tag matches "[0.9]+\\..*") Some(tag)
        else None
      },
      git.useGitDescribe := true,
      licenses := Seq("Apache License v2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      homepage := Some(url("http://github.com/typesafehub/migration-manager")),
      scalacOptions := Seq("-deprecation", "-language:_", "-Xlint")
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
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else                             Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
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

  val compiler = "org.scala-lang" % "scala-compiler" % buildScalaVer
  val swing = "org.scala-lang" % "scala-swing" % buildScalaVer
  val actors = "org.scala-lang" % "scala-actors" % buildScalaVer
  val typesafeConfig = "com.typesafe" % "config" % "1.0.0"

  val specs2 = "org.specs2" %% "specs2" % "2.1.1" % "test"
  val mockito = "org.mockito" % "mockito-all" % "1.9.0" % "test"
  val junit = "junit" % "junit" % "4.7"	% "test"
  def testDeps = List(specs2, mockito, junit)
}

object MimaBuild extends Build {
  import BuildSettings._
  import Dependencies._

  // here we list all projects that are defined.
  override lazy val projects = Seq(root) ++ modules ++ tests :+ reporterFunctionalTests

  lazy val modules = Seq(core, coreui, reporter, reporterui, sbtplugin)

  lazy val root = (
    project("root", file("."))
    aggregate(core, coreui, reporter, reporterui, sbtplugin)
    settings(s3Settings:_*)
    settings(publish := (),
             publishLocal := (),
             mappings in upload <<= (assembly in reporter, assembly in reporterui, version) map { (cli, ui, v) =>
               def loc(name: String) = "migration-manager/%s/%s-%s.jar" format (v, name, v)
               Seq(
                 cli -> loc("migration-manager-cli"),
                 ui  -> loc("migration-manager-ui")
               )
             },
             host in upload := "downloads.typesafe.com.s3.amazonaws.com"
    )
    enablePlugins(GitVersioning)
  )

  lazy val core = (
    project("core", file("core"),
            settings = ((commonSettings ++ buildInfoSettings): Seq[Setting[_]]) ++: Seq(
                sourceGenerators in Compile <+= buildInfo,
                buildInfoKeys := Seq(version),
                buildInfoPackage := "com.typesafe.tools.mima.core.buildinfo",
                buildInfoObject  := "BuildInfo"
                )
           )
    settings(libraryDependencies ++= Seq(compiler) ++ testDeps,
             name := buildName + "-core")
    settings(sonatypePublishSettings:_*)
  )

  lazy val coreui = (
    project("core-ui", file("core-ui"), settings = commonSettings)
    settings(libraryDependencies ++= Seq(actors, swing, compiler) ++ testDeps,
             name := buildName + "-core-ui")
    dependsOn(core)
    settings(sonatypePublishSettings:_*)
  )

  val myAssemblySettings: Seq[Setting[_]] = (assemblySettings: Seq[Setting[_]]) ++ Seq(
     mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
        {
          case "LICENSE" => MergeStrategy.first
          case x => old(x)
        }
     },
     AssemblyKeys.excludedFiles in assembly <<= (AssemblyKeys.excludedFiles in assembly) { (old) =>
       val tmp: Seq[File] => Seq[File] = { files: Seq[File] =>
         // Hack to keep LICENSE files.
         old(files) filterNot (_.getName contains "LICENSE")
       }
       tmp
     }
  )

  lazy val reporter = (
    project("reporter", file("reporter"), settings = commonSettings)
    settings(libraryDependencies ++= Seq(actors, swing, typesafeConfig),
             name := buildName + "-reporter",
             javaOptions += "-Xmx512m")
    dependsOn(core)
    settings(sonatypePublishSettings:_*)
    settings(myAssemblySettings:_*)
    settings(
      // add task functional-tests that depends on all functional tests
      functionalTests <<= runAllTests,
      // make the main 'package' task depend on all functional tests passing  (TODO - Control this in root project...)
      packageBin in Compile <<= packageBin in Compile dependsOn  functionalTests,
      mainClass in assembly := Some("com.typesafe.tools.mima.cli.Main")
    )
  )

  lazy val reporterui = (
    project("reporter-ui", file("reporter-ui"), settings = commonSettings)
    settings(myAssemblySettings:_*)
    settings(sonatypePublishSettings:_*)
    settings(libraryDependencies ++= Seq(swing),
             name := buildName + "-reporter-ui",
             javaOptions += "-Xmx512m",
             mainClass in assembly := Some("com.typesafe.tools.mima.lib.ui.MimaLibApp"))
    dependsOn(coreui, reporter)
  )

  lazy val sbtplugin = (
    Project("sbtplugin", file("sbtplugin"), settings = commonSettings)
    settings(name := "sbt-mima-plugin",
             sbtPlugin := true)
    dependsOn(reporter)
    settings(sbtPublishSettings:_*)
  )

  lazy val reporterFunctionalTests = project("reporter-functional-tests",
  										file("reporter") / "functional-tests" ,
  										settings = commonSettings)
  										.dependsOn(core, reporter)

  // select all testN directories.
  val bases = (file("reporter") / "functional-tests" / "src" / "test") * (DirectoryFilter)

  // make the Project for each discovered directory
  lazy val tests = bases.get map testProject

  // defines a Project for the given base directory (for example, functional-tests/test1)
  // Its name is the directory name (test1) and it has compile+package tasks for sources in v1/ and v2/
  def testProject(base: File) = project(base.name, base, settings = testProjectSettings)
  								  .configs(v1Config, v2Config) dependsOn (reporterFunctionalTests)

  lazy val testProjectSettings =
    commonSettings ++ // normal project defaults; can be trimmed later- test and run aren't needed, for example.
      inConfig(v1Config)(perConfig) ++ // add compile/package for the v1 sources
      inConfig(v2Config)(perConfig) :+  // add compile/package for the v2 sources
      (functionalTests <<= runTest) // add the functional-tests task.

  // this is the key for the task that runs the reporter's functional tests
  lazy val functionalTests = TaskKey[Unit]("test-functional")

  // define configurations for the v1 and v2 sources
  lazy val v1Config = config("v1") extend Compile
  lazy val v2Config = config("v2") extend Compile

  // these are settings defined for each configuration (v1 and v2).
  // We use the normal per-configuration settings, but modify the source directory to be just v1/ instead of src/v1/scala/
  lazy val perConfig = Defaults.configSettings :+ shortSourceDir

  // sets the source directory in this configuration to be: testN / vN
  // scalaSource is the setting key that defines the directory for Scala sources
  // configuration gets the current configuration
  // expanded version: ss <<= (bd, conf) apply { (b,c) => b / c.name }
  lazy val shortSourceDir = scalaSource <<= (baseDirectory, configuration) { _ / _.name }

  // this is the custom test task of the form (ta, tb, tc) map { (a,b,c) => ... }
  // tx are the tasks we need to do our job.
  // Once the task engine runs these tasks, it evaluates the function supplied to map with the task results bound to
  // a,b,c
  lazy val runTest =
    (fullClasspath in (reporterFunctionalTests, Compile), // the test classpath from the functionalTest project for the test
      thisProjectRef, // gives us the ProjectRef this task is defined in
      scalaInstance, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      packageBin in v1Config, // package the v1 sources and get the configuration used
      packageBin in v2Config, // same for v2
      streams) map { (cp, proj, si, v1, v2, streams) =>
        val urls = Attributed.data(cp).map(_.toURI.toURL).toArray
        val loader = new java.net.URLClassLoader(urls, si.loader)

        val testClass = loader.loadClass("com.typesafe.tools.mima.lib.CollectProblemsTest")
        val testRunner = testClass.newInstance().asInstanceOf[{
          def runTest(testClasspath: Array[String], testName: String, oldJarPath: String, newJarPath: String, oraclePath: String): Unit
        }]

        // Add the scala-library to the MiMa classpath used to run this test
        val testClasspath = Attributed.data(cp).filter(_.getName endsWith "scala-library.jar").map(_.getAbsolutePath).toArray

        val projectPath = proj.build.getPath + "reporter" + "/" + "functional-tests" + "/" + "src" + "/" + "test" + "/" + proj.project

        val oraclePath = projectPath + "/problems.txt"

        try {
          testRunner.runTest(testClasspath, proj.project, v1.getAbsolutePath, v2.getAbsolutePath, oraclePath)
          streams.log.info("Test '" + proj.project + "' succeeded.")
        } catch {
          case e: Exception =>  streams.log.error(e.toString)
        }
        ()
      }

  lazy val runAllTests =
    (state, // this is how we access all defined projects from a task
      thisProjectRef, // gives us the ProjectRef this task is defined in
      test in Test // requires unit tests to run first
      ) flatMap { (s, proj, _) =>
        // gets all defined projects, dropping this project (core) so the task doesn't depend on itself
        val structure = Project.structure(s)
        val allProjects = structure.units(proj.build).defined.values filter (_.id != proj.project)
        // get the fun-tests task in each project
        val allTests = allProjects.toSeq flatMap { p => functionalTests in ProjectRef(proj.build, p.id) get structure.data }
        // depend on all fun-tests
        allTests.join.map(_ => ())
      }

    def project(id: String, base: File, settings: Seq[Def.Setting[_]] = Nil) =
      Project(id, base, settings = settings) disablePlugins(BintrayPlugin)
}
