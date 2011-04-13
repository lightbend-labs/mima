import sbt._
import Keys._

// I need to make these imported by default
import Project.inConfig
import Configurations.config
import Build.data

object MimaBuild extends Build {
  // here we list all projects that are defined.
  // core is a fixed Project, tests are discovered by listing directories
  lazy val projects = core +: tests

  // core contains the main migration manager code in src/main/scala and the test code in src/test/scala
  //  configs shouldn't be necessary: works around a bug in sbt
  lazy val core: Project = Project("core", file("core"))
    .configs(v1Config, v2Config)
    .settings(
      // add fun-tests that depends on all functional tests
      funTests <<= runAllTests,
      // make the main 'package' task depend on functional tests passing
      packageBin in Compile <<= packageBin in Compile dependsOn funTests)

  // select all testN directories.
  lazy val bases = file("functional-tests") * (DirectoryFilter)

  // make the Project for each discovered directory
  lazy val tests = bases.getFiles map testProject

  // defines a Project for the given base directory (for example, functional-tests/test1)
  //  Its name is the directory name (test1) and it has compile+package tasks for sources in v1/ and v2/
  def testProject(base: File) =
    Project(base.name, base, settings = testProjectSettings).configs(v1Config, v2Config)

  lazy val testProjectSettings =
    Defaults.defaultSettings ++ // normal project defaults; can be trimmed later- test and run aren't needed, for example.
      inConfig(v1Config)(perConfig) ++ // add compile/package for the v1 sources
      inConfig(v2Config)(perConfig) :+ // add compile/package for the v2 sources
      (funTests <<= runTests) // add the fun-tests task.

  // this is the key for the task that runs a test
  lazy val funTests = TaskKey[Unit]("fun-tests")

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
  lazy val runTests =
    (fullClasspath in (core, Test), // the test classpath from the core project for the test
      thisProjectRef, // gives us the ProjectRef this task is defined in
      scalaInstance, // get a reference to the already loaded Scala classes so we get the advantage of a warm jvm
      packageBin in v1Config, // package the v1 sources and get the configuration used
      packageBin in v2Config // same for v2
      ) map { (cp, proj, si, v1, v2) =>
        val urls = data(cp).map(_.toURI.toURL).toArray
        val loader = new java.net.URLClassLoader(urls, si.loader)

        val testClass = loader.loadClass("ssol.tools.mima.ui.CollectProblemsTest")
        val testRunner = testClass.newInstance().asInstanceOf[{ def runTest(testName: String, oldJarPath: String, newJarPath: String, oraclePath: String): Unit }]

        val projectPath = proj.build.getPath + "functional-tests" + "/" + proj.project
        val oraclePath = projectPath + "/problems.txt"

        try {
          testRunner.runTest(proj.project, v1.jar.getAbsolutePath, v2.jar.getAbsolutePath, oraclePath)
        } catch {
          case e => println("[fatal] Failed to run Test '" + proj.project + "'")
        }

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
        val allTests = allProjects.toSeq flatMap { p => funTests in ProjectRef(proj.build, p.id) get structure.data }
        // depend on all fun-tests
        allTests.join.map(_ => ())
      }
}