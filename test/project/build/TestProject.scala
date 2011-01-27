import sbt._

class TestProject(info: ProjectInfo) extends ParentProject(info) {
  lazy val mutualRefs = project("mutual-refs", "Mutual References Test", new VersionsProject(_))
   
  /** Adds an action 'package', that asks all sub-projects to package their versions and copy them to their target/ */
  lazy val `package` = task { None } dependsOn (mutualRefs.packageVersions)

  class VersionsProject(info: ProjectInfo) extends ParentProject(info) {
    lazy val v1 = project("v1", "Old Version", VersionProject("v1") _)
    lazy val v2 = project("v2", "New Version", VersionProject("v2") _)

    /** Adds an action 'package-versions' to this project. It copies the jar files in 'target/' */
    lazy val packageVersions = task { 
      FileUtilities.copyFlat(List(v1.jarPath, v2.jarPath), outputRootPath, log) match {
        case Left(e) => Some(e.toString)
        case Right(e) => None 
      }
    } dependsOn (v1.`package`, v1.`package`)

    case class VersionProject(ver: String)(info: ProjectInfo) extends DefaultProject(info) {
      
//      override def mainScalaSourcePath = 
    }
  }

}
