package ssol.tools
package mima

import Config._

import util.IndentedOutput._
import scala.tools.nsc.io.{File, AbstractFile}
import scala.tools.nsc.util.{DirectoryClassPath, JavaClassPath}
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
    case None => fatal("not a directory or jar file: "+name)
  }

  private val problems = new ListBuffer[Problem]

  private def raise(problem: Problem) = {
    problems += problem
    info("Problem: "+problem.description+(
      if (problem.status != Problem.Status.Unfixable) " ("+problem.status+")" else ""))
  }

  private def uniques(methods: List[MemberInfo]): List[MemberInfo] = 
    methods.groupBy(_.parametersSig).values.map(_.head).toList

  private def compareClasses(oldclazz: ClassInfo, newclazz: ClassInfo) {
    info("[compare] %s \t %s".format(oldclazz, newclazz))
    for (oldfld <- oldclazz.fields.iterator)
      if (oldfld.isAccessible) {
        val newflds = newclazz.lookupFields(oldfld.name)
        if (newflds.hasNext) {
          val newfld = newflds.next
          if (!newfld.isPublic) 
            raise(InaccessibleFieldProblem(newfld))
          else if (oldfld.sig != newfld.sig)
            raise(IncompatibleFieldTypeProblem(oldfld, newfld))
        } else 
          raise(MissingFieldProblem(oldfld))
      }
    for (oldmeth <- oldclazz.methods.iterator)
      if (oldmeth.isAccessible) {
        val newmeths = newclazz.lookupMethods(oldmeth.name).toList
        if (newmeths.nonEmpty)
          newmeths find (_.sig == oldmeth.sig) match {
            case None =>
              newmeths find (oldmeth matchesType _) match {
                case None =>
                  raise(IncompatibleMethTypeProblem(oldmeth, uniques(newmeths)))
                case Some(newmeth) =>
                  raise(IncompatibleResultTypeProblem(oldmeth, newmeth))
                }
            case Some(newmeth) =>
              if (!newmeth.isPublic) 
                raise(InaccessibleMethodProblem(newmeth))
          }
        else 
          raise(MissingMethodProblem(oldmeth))
      }
    for (newmeth <- newclazz.methods.iterator)
      if (newmeth.isDeferred) {
        val oldmeths = oldclazz.methods.get(newmeth.name)
        oldmeths find (_.sig == newmeth.sig) match {
          case Some(oldmeth) if oldmeth.isDeferred =>
            ;
          case _ =>
            raise(AbstractMethodProblem(newmeth))
        }
      }        
  }          

  private def comparePackages(oldpkg: PackageInfo, newpkg: PackageInfo) {
    val traits = newpkg.traits // determine traits of new package first
    for (oldclazz <- oldpkg.accessibleClasses) {
      newpkg.classes get oldclazz.name match {
        case None =>
          raise(MissingClassProblem(oldclazz))
        case Some(newclazz) =>
          if (!newclazz.isPublic)
            raise(InaccessibleClassProblem(newclazz))
          compareClasses(oldclazz, newclazz)
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
            raise(MissingPackageProblem(oldpkg))
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
    info("[old version in: "+oldRoot+"]")
    info("[new version in: "+newRoot+"]")
    info("classpath: " + Config.baseClassPath.asClasspathString)
    traversePackages(oldRoot.targetPackage, newRoot.targetPackage)
    val fixes = new ListBuffer[Fix]
    problems.toList
  }
  
  /** Return a list of fixes for the given problems. */
  private def fixesFor(problems: List[IncompatibleResultTypeProblem]): List[Fix] = {
  	val fixes = new ListBuffer[Fix]
  	
    for ((clazz, problems) <- problems groupBy (_.newmeth.owner)) {
      fixes += new Fix(clazz).lib(problems map (p => (p.newmeth, p.oldmeth.sig)))
    } 
  	fixes.toList
  }
}

object MiMaLib extends MiMaLib {
  def main(args: Array[String]) = {
//    setupClassfileParser()
    setup("scala ssol.tools.misco.MiMaLib <old-dir> <new-dir>", args, _.length == 2, "-fixall")
    val oldRoot = root(Config.oldLib.get.getAbsolutePath)
    val newRoot = root(Config.newLib.get.getAbsolutePath)      
    info("[old version in: "+oldRoot+"]")
    info("[new version in: "+newRoot+"]")
    traversePackages(oldRoot.targetPackage, newRoot.targetPackage)
    val fixes = new ListBuffer[Fix]
    if (Config.fixall) {
      val resultTypeProblems = problems.toList collect { case x: IncompatibleResultTypeProblem => x }
      for ((clazz, problems) <- resultTypeProblems groupBy (_.newmeth.owner)) {
        fixes += new Fix(clazz).lib(problems map (p => (p.newmeth, p.oldmeth.sig)))
      } 
      new Writer(fixes).writeOut()
    }
  }
}
