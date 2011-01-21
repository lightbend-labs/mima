package ssol.tools
package mima

import Config._
import util.IndentedOutput._
import scala.tools.nsc.io.{File, AbstractFile}
import scala.tools.nsc.util.DirectoryClassPath
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

  def classPath(name: String) = {
    val f = new File(new java.io.File(name))
    val dir = AbstractFile.getDirectory(f)
    if (dir == null) null
    else new DirectoryClassPath(dir, DefaultJavaContext)
  }

  def root(name: String) = {
    val cp = classPath(name)
    if (cp == null) Config.fatal("not a directory or jar file: "+name)
    else new ConcretePackageInfo(null, cp)
  }

  def setupClassfileParser() {
    ClassfileParser.readFields = (clazz: ClassInfo) => true
    ClassfileParser.readMethods = (clazz: ClassInfo) => true
    ClassfileParser.readCode = (meth: MemberInfo) => false
  }

  val problems = new ListBuffer[Problem]

  def raise(problem: Problem) = {
    problems += problem
    println("Problem: "+problem.description+(
      if (problem.status != Problem.Status.Unfixable) " ("+problem.status+")" else ""))
  }

  private def uniques(methods: List[MemberInfo]): List[MemberInfo] = 
    methods.groupBy(_.parametersSig).values.map(_.head).toList

  def compareClasses(oldclazz: ClassInfo, newclazz: ClassInfo) {
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

  def comparePackages(oldpkg: PackageInfo, newpkg: PackageInfo) {
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

  def traversePackages(oldpkg: PackageInfo, newpkg: PackageInfo) {
    if (settings.verbose.value) 
      printLine("* " + oldpkg.fullName + ": ")
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
    Config.info("[old version in: "+oldRoot+"]")
    Config.info("[new version in: "+newRoot+"]")
    traversePackages(oldRoot, newRoot)
    val fixes = new ListBuffer[Fix]
    problems.toList
  }
  
  /** Return a list of fixes for the given problems. */
  def fixesFor(problems: List[IncompatibleResultTypeProblem]): List[Fix] = {
  	val fixes = new ListBuffer[Fix]
  	
    for ((clazz, problems) <- problems groupBy (_.newmeth.owner)) {
      fixes += new Fix(clazz).lib(problems map (p => (p.newmeth, p.oldmeth.sig)))
    } 
  	fixes.toList
  }
}

object MiMaLib extends MiMaLib {
  def main(args: Array[String]) = {
    setupClassfileParser()
    val resargs = Config.setup("scala ssol.tools.misco.MiMaLib <old-dir> <new-dir>", args, _.length == 2, "-fixall")
    val oldRoot = root(resargs(0))
    val newRoot = root(resargs(1))      
    Config.info("[old version in: "+oldRoot+"]")
    Config.info("[new version in: "+newRoot+"]")
    traversePackages(oldRoot, newRoot)
    val fixes = new ListBuffer[Fix]
    if (Config.settings.fixall.value) {
      val resultTypeProblems = problems.toList collect { case x: IncompatibleResultTypeProblem => x }
      for ((clazz, problems) <- resultTypeProblems groupBy (_.newmeth.owner)) {
        fixes += new Fix(clazz).lib(problems map (p => (p.newmeth, p.oldmeth.sig)))
      } 
      new Writer(fixes).writeOut()
    }
  }
}
