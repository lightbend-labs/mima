package ssol.tools.mima.client

import scala.tools.nsc.io.{AbstractFile, PlainFile, ZipArchive}
import java.io._
import java.util.zip._
import collection.mutable

import scala.tools.nsc.{ util, io }
import util._
import ClassPath._

import ssol.tools.mima.core._
import ssol.tools.mima.core.util.IndentedOutput._

object MiMaClient {
  def apply(cp: JavaClassPath): List[Fix] = {
    val baseDef = new Definitions(None, cp)
    val mimaClient = new MiMaClient
    mimaClient.traversePackage(baseDef.root)
    mimaClient.fixes.toList
  }
}

class MiMaClient {
  import ssol.tools.mima.core.util.log.ConsoleLogging._
  import PackageInfo._

  private var ignore = Set("java", "javax", "sun")

  private val fixes = new mutable.ListBuffer[Fix]

  private def processClass(clazz: ClassInfo) {
    if (Config.debug) {
      printLine("    "+clazz+" extends "+clazz.superClass.name+
                " implements "+clazz.interfaces.map(_.name).mkString(", "))
      if (clazz.isTrait)
        for (m <- clazz.concreteMethods)
          printLine("      "+m)
    }
    if (clazz.unimplementedMethods.nonEmpty || clazz.unimplementedSetters.nonEmpty) {
      info(clazz+" at "+clazz.file+" needs to be fixed")
      if (clazz.unimplementedMethods.nonEmpty)
        info("  has unimplemented methods: \n    "+clazz.unimplementedMethods.map(_.description).mkString("\n    "))
      if (clazz.unimplementedSetters.nonEmpty)
        info("  has unimplemented fields with setters:\n   "+clazz.unimplementedSetters.map(_.description).mkString("\n    "))
      info(clazz.superClass.description)
      info(clazz.directTraits map (_.description) toString)
      info(clazz.superClass.allTraits map (_.description) toString)
      info(clazz.superClass.allTraits map (_.interfaces) toString)
      fixes += new ClientFix(clazz).fix()
    }
  }

  private def processPackage(pkg: PackageInfo) {
    def printKeys[T](lead: String, m: collection.Map[String, T]) = printLine(lead+ m.keys.mkString(", "))
    if (Config.debug) {
      printKeys("  implClasses = ", pkg.implClasses)
      printKeys("  traits      = ", pkg.traits)
      printKeys("  classes     = ", pkg.classes)
    }
    val traits = pkg.traits // determine traits first
    for ((_, clazz) <- pkg.classes) processClass(clazz)
  }

  private def traversePackage(pkg: PackageInfo): Unit =
    if (!(ignore contains pkg.fullName)) {
      info("* " + pkg.fullName + ": ")
      processPackage(pkg)
      indented {
        pkg.packages.valuesIterator foreach traversePackage
      }
    }

  def main(args: Array[String]) {
    Config.setup("scala ssol.mima.MiMaClient", args, _.isEmpty)
    // TODO: Check this is the right base classpath to give it
    traversePackage(Config.baseDefinitions.root)
    //new Writer(fixes).writeOut()
  }
}
