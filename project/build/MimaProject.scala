import sbt._

class MimaProject(info: ProjectInfo) extends ParentProject(info) {

  // Add Maven Local repository for SBT to search for (disable if this doesn't suit you)
  val mavenLocal = "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository"
  override def managedStyle = ManagedStyle.Maven
  
  lazy val core = project("core", "Mima Core", new MimaCore(_))
  
  def functionalTestProject(path: Path) = 
  	project(outputRootPath / functionalTestsPath.name / path.name, path.name, new FunctionalTest(_), core)

  /** functional test directory */
  lazy val functionalTestsPath = path("functional-tests")
  /** collect all children folders of "functional-tests" */
	lazy val functionalTestsDirs = functionalTestsPath.*(AllPassFilter).strictMap(_.relativePath.split("/").toList.last)
	/** create a subproject for each collected test directory */
	lazy val functionalTestsProjects = functionalTestsDirs.map(functionalTestProject(_))
	
  /** Adds an action 'package', that asks all sub-projects to package their versions and copy them to their target/ */
  lazy val `package` = task { None } dependsOn (functionalTestsProjects.map(project => project.packageVersions).toArray : _*)

  
  class FunctionalTest(info: ProjectInfo) extends ParentProject(info) {
    lazy val v1 = project("v1", "Old Version", VersionProject("v1") _)
    lazy val v2 = project("v2", "New Version", VersionProject("v2") _)

    /** Adds an action 'package-versions' to this project. It copies the jar files in 'target/' */
    lazy val packageVersions = task {
    	FileUtilities.copyFlat(List(v1.jarPath, v2.jarPath), ".", log) match {
        case Left(e) => Some(e.toString)
        case Right(e) => None 
      }
    } dependsOn (v1.`package`, v2.`package`)

    case class VersionProject(ver: String)(info: ProjectInfo) extends DefaultProject(info) {
    }
  }

  class MimaCore(info: ProjectInfo) extends DefaultProject(info) with SwingDependencies
  
  trait SwingDependencies {
    val scalaSwing = "org.scala-lang" % "scala-swing" % "2.8.1" % "compile"
  }

}
