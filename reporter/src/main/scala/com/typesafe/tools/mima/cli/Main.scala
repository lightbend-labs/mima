package com.typesafe.tools.mima
package cli

import lib.MiMaLib
import scala.tools.cmd._
import scala.tools.nsc.util.{JavaClassPath,ClassPath}
import ClassPath.DefaultJavaContext
import core.Config

/** A program to run the MIMA tools from the command line.
 */
trait MimaSpec extends Spec with Meta.StdOpts with Interpolation {
  lazy val referenceSpec  = MimaSpec
  lazy val programInfo    = Spec.Info("demo", "Usage: mima [<options>]", "com.typesafe.tools.mima.Main")

  help("""Usage: mima [<options>]""")
  heading("necessary options:")
  val prevfile = "prev" / "Previous classpath/jar for binary compatibility testing." defaultTo ""
  val currentfile = "curr" / "Current classpath/jar for binary compatibility testing." defaultTo ""
  heading("optional settings:")
  val classpath    = "classpath"    / "an optional classpath setting"        defaultTo System.getProperty("java.class.path")
}

object MimaSpec extends MimaSpec with Property {
  lazy val propMapper = new PropertyMapper(MimaSpec)

  type ThisCommandLine = SpecCommandLine
  def creator(args: List[String]) =
    new SpecCommandLine(args) {
      override def errorFn(msg: String) = { println("Error: " + msg) ; sys.exit(0) }
    }
}

/** Helper method for Mima Reporter runner. */
class Main(args: List[String]) extends {
  val parsed = MimaSpec(args: _*)
} with MimaSpec with Instance {

  def helpMsg = MimaSpec.helpMsg

  val (curr, prev) =
    (currentfile, prevfile) match {
      case ("", "")   if residualArgs.size == 2 => (residualArgs.head, residualArgs.tail.head)
      case ("", last) if residualArgs.size == 1 => (residualArgs.head, last)
      case (head, _)  if residualArgs.size == 1 => (head, residualArgs.head)
      case other                                => other
    }

  def isDefined: Boolean = !(curr.isEmpty || prev.isEmpty)

  def makeClasspath = new JavaClassPath(
      // TODO expand path?
      DefaultJavaContext.classesInPath(classpath).toIndexedSeq, DefaultJavaContext)

  def makeMima = {
    // TODO: get mima to use paul's CMD library *or*
    // hack from one to the other temporarily.
    Config.setup("mima", Array.empty)
    new MiMaLib(makeClasspath)
  }

  /** Converts a problem to a human-readable mapped string. */
  private def printProblem(p: core.Problem): String = {
    def wrap(words: Seq[String], result: List[String] = Nil): Seq[String] =
      if(words.isEmpty) result.reverse
      else {
        // This is so painfully slow, it hurts.
        val output = {
          val tmp = (words.inits dropWhile { x => x.map(_.length).sum + x.length > 77 }).next
          if(tmp.isEmpty) Seq(words.head)
          else tmp
        }
        val line = output mkString " "
        val rest = words drop output.length
        wrap(rest, line :: result)
      }
    def wrapString(s: String) = wrap(s split "\\s")
    wrapString(" * " + p.description) mkString "\n   "
  }

  def run(): Int = {
    val mima = makeMima
    val problems = mima.collectProblems(prevfile, currentfile)
    val header = "Found " + problems.size + " binary incompatibiities"
    println(header)
    println(Seq.fill(header.length)("=") mkString "")
    problems map printProblem foreach println
    problems.size
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    val runner = new Main(args.toList)

    if (args.isEmpty || !runner.isDefined) println(runner.helpMsg)
    else                                   System.exit(runner.run())
  }
}
