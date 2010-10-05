package ssol.tools
package mima

import scala.tools.nsc.io.{AbstractFile, PlainFile, ZipArchive}
import java.io._
import java.util.zip._
import util.IndentedOutput._
import collection.mutable

object MiMaClient {
  import PackageInfo._

  private var ignore = Set("java", "javax", "sun")

  private val fixes = new mutable.ListBuffer[Fix]

  def processClass(clazz: ClassInfo) {
    if (Config.settings.debug.value) {
      printLine("    "+clazz+" extends "+clazz.superClass.name+
                " implements "+clazz.interfaces.map(_.name).mkString(", "))
      if (clazz.isTrait)
        for (m <- clazz.concreteMethods)
          printLine("      "+m)
    }
    if (clazz.unimplementedMethods.nonEmpty || clazz.unimplementedSetters.nonEmpty) {
      println(clazz+" at "+clazz.file+" needs to be fixed")
      if (clazz.unimplementedMethods.nonEmpty)
        println("  has unimplemented methods: \n    "+clazz.unimplementedMethods.map(_.description).mkString("\n    "))
      if (clazz.unimplementedSetters.nonEmpty)
        println("  has unimplemented fields with setters:\n   "+clazz.unimplementedSetters.map(_.description).mkString("\n    "))
      println(clazz.superClass.description)
      println(clazz.directTraits map (_.description))
      println(clazz.superClass.allTraits map (_.description))
      println(clazz.superClass.allTraits map (_.interfaces))
      fixes += new Fix(clazz).client()
    }
  }

  def processPackage(pkg: PackageInfo) {
    def printKeys[T](lead: String, m: collection.Map[String, T]) = printLine(lead+ m.keys.mkString(", "))
    if (Config.settings.debug.value) {
      printKeys("  implClasses = ", pkg.implClasses)
      printKeys("  traits      = ", pkg.traits)
      printKeys("  classes     = ", pkg.classes)
    }
    val traits = pkg.traits // determine traits first
    for ((_, clazz) <- pkg.classes) processClass(clazz)
  }

  def traversePackage(pkg: PackageInfo): Unit =
    if (!(ignore contains pkg.fullName)) {
      if (Config.settings.verbose.value) 
        printLine("* " + pkg.fullName + ": ")
      processPackage(pkg)
      indented {
        pkg.packages.valuesIterator foreach traversePackage
      }
    }

  def main(args: Array[String]) {
    Config.setup("scala ssol.mima.MiMaClient", args, _.isEmpty)
    traversePackage(root)
    new Writer(fixes).writeOut()
  }
}
