package com.typesafe.tools.mima
package cli

import java.io.File

import com.typesafe.tools.mima.core.{Config, ProblemFilter}
import com.typesafe.tools.mima.lib.MiMaLib

import scala.tools.cmd._

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
  val problemFilters = "filters" / "an optional problem filters configuration file, applied to both backward and forward checking" --|
  val backwardFilters = "backward-filters" / "an optional problem filters configuration file, only applied to backward checking" --|
  val forwardFilters = "forward-filters" / "an optional problem filters configuration file, only applied to forward checking" --|
  val generateFilters = "generate-filters" / "generate filters definition for displayed problems" --?
  val direction = "direction" / "check direction, default is \"backward\", but can also be \"forward\" or \"both\"" --|
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

  def makeClasspath = com.typesafe.tools.mima.core.reporterClassPath(classpath) // TODO expand path?

  def makeMima = {
    // TODO: get mima to use paul's CMD library *or*
    // hack from one to the other temporarily.
    Config.setup("mima", Array.empty)
    new MiMaLib(makeClasspath)
  }

  /** Converts a problem to a human-readable mapped string. */
  private def printProblem(p: core.Problem, affected: String): String = {
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
    wrapString(" * " + p.description(affected)) mkString "\n   "
  }

  private def loadFilters(configFile: File): Seq[ProblemFilter] = {
    import com.typesafe.config._
    try {
      val fallback = ConfigFactory.parseString("filter { problems = []\npackages=[] }")
      val config: Config =
        ConfigFactory.parseFile(configFile).withFallback(fallback).resolve()
      ProblemFiltersConfig.parseProblemFilters(config)
    } catch {
      case e: Exception =>
        Console.err.println("Problem with loading filter configuration:")
        e.printStackTrace(Console.err)
        System.exit(1)
        // we rethrow to satisfy types, System.exit should do the job
        throw e
    }
  }

  private def printGeneratedFilters(errors: Seq[core.Problem], direction: String): Unit = {
    val errorsFilterConfig = ProblemFiltersConfig.problemsToProblemFilterConfig(errors)
    val header = "\nGenerated " + direction + " filter config definition"
    println(header)
    println(Seq.fill(header.length)("=") mkString "")
    import com.typesafe.config._
    val renderOptions = ConfigRenderOptions.defaults().setOriginComments(false).setJson(false)
    println(errorsFilterConfig.root.render(renderOptions))
  }

  def parseFilters(os:Option[String]) = os.toSeq.map(filePath => loadFilters(new File(filePath))).flatten

  def run(): Int = {
    val effectiveDirection = direction.getOrElse("backwards")
    // MiMaLib collects problems to a mutable buffer, therefore we need a new instance every time
    val backwardProblems = effectiveDirection match {
      case "backward" | "backwards" | "both" => makeMima.collectProblems(prevfile, currentfile)
      case _ => Nil
    }
    val forwardProblems = effectiveDirection match {
      case "forward" | "forwards" | "both" => makeMima.collectProblems(currentfile, prevfile)
      case _ => Nil
    }
    val bothFilters = parseFilters(problemFilters)
    val backFilters = bothFilters ++ parseFilters(backwardFilters)
    val forwFilters = bothFilters ++ parseFilters(forwardFilters)
    def isReported(problem: core.Problem, filters: Seq[core.Problem => Boolean]) = filters.forall(filter => filter(problem))
    val backErrors = backwardProblems.filter(isReported(_,backFilters))
    val forwErrors = forwardProblems.filter(isReported(_,forwFilters))
    val errorsSize = backErrors.size + forwErrors.size
    val header = "Found " + errorsSize + " binary incompatibilities" + {
      val filteredOutSize = backwardProblems.size + forwardProblems.size - errorsSize
      if (filteredOutSize > 0) " (" + filteredOutSize + " were filtered out)" else ""
    }
    println(header)
    println(Seq.fill(header.length)("=") mkString "")
    backErrors map {p:core.Problem => printProblem(p,"current")} foreach println
    forwErrors map {p:core.Problem => printProblem(p,"other")} foreach println
    if (generateFilters) {
      printGeneratedFilters(backErrors, "backward")
      printGeneratedFilters(forwErrors, "forward")
    }
    errorsSize
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    val runner = new Main(args.toList)

    if (args.isEmpty || !runner.isDefined) println(runner.helpMsg)
    else                                   System.exit(runner.run())
  }
}
