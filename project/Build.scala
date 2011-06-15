import sbt._
import Keys._

// I need to make these imported by default
import Project.inConfig
import Configurations.config
import Build.data
import Path._

object BuildSettings {
  
  val buildName = "mima"
  val buildOrganization = "com.typesafe"
  val buildScalaVer = "2.9.0-1"
  val buildVersion = "0.1.1"
  
  val commonSettings = Defaults.defaultSettings ++ Seq (
      organization := buildOrganization,
      scalaVersion := buildScalaVer,
      version      := buildVersion
  )
}

object Dependencies {
  import BuildSettings._
  
  val compiler = "org.scala-lang" % "scala-compiler" % buildScalaVer
  val swing = "org.scala-lang" % "scala-swing" % buildScalaVer
  
  val specs2 = "org.specs2" %% "specs2" % "1.4" % "test"
}

object MimaBuild extends Build {
  import BuildSettings._
  import Dependencies._

  // here we list all projects that are defined.
  override lazy val projects = Seq(root) ++ modules
  
  lazy val modules = Seq(core, reporter, migrator, reporterFunctionalTests) ++ tests

  lazy val root = Project("root", file("."), aggregate = modules.map(Reference.projectToRef(_)))

  lazy val core = Project("core", file("core"), settings = commonSettings  ++ 
  								Seq(libraryDependencies ++= Seq(swing, compiler, specs2)) :+ 
  								(name := buildName + "-core"))

  lazy val reporter = Project("reporter", file("reporter"), 
  	 settings = commonSettings ++ Seq(libraryDependencies ++= Seq(swing)) :+ 
  	 				(name := buildName + "-reporter") :+ (javaOptions += "-Xmx512m"))
	.dependsOn(core)
	.settings(
	  // add task functional-tests that depends on all functional tests
	  functionalTests <<= runAllTests,
      // make the main 'package' task depend on all functional tests passing
      packageBin in Compile <<= packageBin in Compile dependsOn  functionalTests
	)	

  lazy val reporterFunctionalTests = Project("reporter-functional-tests", 
  										file("reporter") / "functional-tests" , 
  										settings = commonSettings)
  										.dependsOn(core, reporter)

  lazy val migrator = Project("migrator", file("migrator"), 
  						settings = commonSettings ++ 
  								   Seq(libraryDependencies ++= Seq(swing, compiler)) :+ 
  							       (name := buildName + "-migrator") :+ (javaOptions += "-Xmx512m"))
  						.dependsOn(core, reporter)

  // select all testN directories.
  val bases = (file("reporter") / "functional-tests" / "src" / "test") * (DirectoryFilter)

  // make the Project for each discovered directory
  lazy val tests = bases.getFiles map testProject

  // defines a Project for the given base directory (for example, functional-tests/test1)
  // Its name is the directory name (test1) and it has compile+package tasks for sources in v1/ and v2/
  def testProject(base: File) = Project(base.name, base, settings = testProjectSettings)
  								  .configs(v1Config, v2Config) dependsOn (reporterFunctionalTests)

  lazy val testProjectSettings =
    commonSettings ++ // normal project defaults; can be trimmed later- test and run aren't needed, for example.
      inConfig(v1Config)(perConfig) ++ // add compile/package for the v1 sources
      inConfig(v2Config)(perConfig) :+  // add compile/package for the v2 sources
      (functionalTests <<= runTest) // add the functional-tests task.

  // this is the key for the task that runs the reporter's functional tests
  lazy val functionalTests = TaskKey[Unit]("test-functional")

  // define configurations for the v1 and v2 sources
  lazy val v1Config = config("v1")
  lazy val v2Config = config("v2")

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
        val urls = data(cp).map(_.toURI.toURL).toArray
        val loader = new java.net.URLClassLoader(urls, si.loader)

        val testClass = loader.loadClass("ssol.tools.mima.lib.CollectProblemsTest")
        val testRunner = testClass.newInstance().asInstanceOf[
        					{ def runTest(testName: String, oldJarPath: String, newJarPath: String, 
        							oraclePath: String): Unit 
        					}]

        val projectPath = proj.build.getPath + "reporter" + "/" + "functional-tests" + "/" + "src" +
        					"/" + "test" + "/" + proj.project
        					
        val oraclePath = projectPath + "/problems.txt"

        try {
          testRunner.runTest(proj.project, v1.getAbsolutePath, v2.getAbsolutePath, oraclePath)
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

}
