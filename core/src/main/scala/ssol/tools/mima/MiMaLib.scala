package ssol.tools
package mima


import Config._
import analyze.Analyzer
import util.IndentedOutput._
import scala.tools.nsc.io.{ File, AbstractFile }
import scala.tools.nsc.util.{ DirectoryClassPath, JavaClassPath }
import collection.mutable.ListBuffer

class MiMaLib {

  /*
  options: 
  -classpath foo
  -ignore bar
  -d outputdir
  -i, -iinteractive
  -f, -fix
  */
  
  private def classPath(name: String) = {
    val f = new File(new java.io.File(name))
    val dir = AbstractFile.getDirectory(f)
    if (dir == null) None
    else Some(new DirectoryClassPath(dir, DefaultJavaContext))
  }

  private def root(name: String): Definitions = classPath(name) match {
    case cp @ Some(_) => new Definitions(cp, Config.baseClassPath)
    case None         => fatal("not a directory or jar file: " + name)
  }

  private val problems = new ListBuffer[Problem]

  private def raise(problem: Problem) = {
    problems += problem
    info("Problem: " + problem.description + (if (problem.status != Problem.Status.Unfixable) " (" + problem.status + ")" else ""))
  }


  private def comparePackages(oldpkg: PackageInfo, newpkg: PackageInfo) {
    val traits = newpkg.traits // determine traits of new package first
    for (oldclazz <- oldpkg.accessibleClasses) {
      newpkg.classes get oldclazz.name match {
        case None if oldclazz.isImplClass =>
          // if it is missing a trait implementation class, then no error should be reported 
          // since there should be already errors, i.e., missing methods...
          ()
        
        case None => raise(MissingClassProblem(oldclazz))
        
        case Some(newclazz) =>  Analyzer(oldclazz, newclazz).foreach(raise)
      }
    }
  }

  private def traversePackages(oldpkg: PackageInfo, newpkg: PackageInfo) {
    info("* " + oldpkg.fullName + ": ")
    comparePackages(oldpkg, newpkg)
    indented {
      for (p <- oldpkg.packages.valuesIterator) {
        newpkg.packages get p.name match {
          case None =>
            traversePackages(p, NoPackageInfo)
          case Some(q) =>
            traversePackages(p, q)
        }
      }
    }
  }

  /** Return a list of problems for the two versions of the library. */
  def collectProblems(oldDir: String, newDir: String): List[Problem] = {
    val oldRoot = root(oldDir)
    val newRoot = root(newDir)
    info("[old version in: " + oldRoot + "]")
    info("[new version in: " + newRoot + "]")
    info("classpath: " + Config.baseClassPath.asClasspathString)
    traversePackages(oldRoot.targetPackage, newRoot.targetPackage)
    problems.toList
  }
}
