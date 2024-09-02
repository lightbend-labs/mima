package com.typesafe.tools.mima.cli

import com.typesafe.tools.mima.lib.MiMaLib

import java.io.File
import scala.annotation.tailrec

case class Main(
    classpath: Seq[File] = Nil,
    oldBinOpt: Option[File] = None,
    newBinOpt: Option[File] = None,
    formatter: ProblemFormatter = ProblemFormatter()
) {

  def run(): Int = {
    val oldBin = oldBinOpt.getOrElse(
      throw new IllegalArgumentException("Old binary was not specified")
    )
    val newBin = newBinOpt.getOrElse(
      throw new IllegalArgumentException("New binary was not specified")
    )
    // TODO: should have some machine-readable output here, as an option
    val problems = new MiMaLib(classpath)
      .collectProblems(oldBin, newBin, Nil)
      .flatMap(formatter.formatProblem)
    problems.foreach(println)
    problems.size
  }

}

object Main {

  def main(args: Array[String]): Unit =
    try System.exit(parseArgs(args.toList, Main()).run())
    catch {
      case err: IllegalArgumentException =>
        println(err.getMessage())
        printUsage()
    }

  def printUsage(): Unit = println(
    s"""Usage:
      |
      |mima [OPTIONS] oldfile newfile
      |
      |  oldfile: Old (or, previous) files - a JAR or a directory containing classfiles
      |  newfile: New (or, current) files - a JAR or a directory containing classfiles
      |
      |Options:
      |  -cp CLASSPATH:
      |     Specify Java classpath, separated by '${File.pathSeparatorChar}'
      |
      |  -v, --verbose:
      |     Show a human-readable description of each problem
      |
      |  -f, --forward-only:
      |    Show only forward-binary-compatibility problems
      |
      |  -b, --backward-only:
      |    Show only backward-binary-compatibility problems
      |
      |  -g, --include-generics:
      |    Include generic signature problems, which may not directly cause bincompat
      |    problems and are hidden by default. Has no effect if using --forward-only.
      |
      |  -j, --bytecode-names:
      |    Show bytecode names of fields and methods, rather than human-readable names
      |
      |""".stripMargin
  )

  @tailrec
  private def parseArgs(remaining: List[String], current: Main): Main =
    remaining match {
      case Nil => current
      case ("-cp" | "--classpath") :: cpStr :: rest =>
        parseArgs(
          rest,
          current.copy(classpath =
            cpStr.split(File.pathSeparatorChar).toSeq.map(new File(_))
          )
        )

      case ("-f" | "--forward-only") :: rest =>
        parseArgs(
          rest,
          current.copy(formatter =
            current.formatter.copy(showForward = true, showBackward = false)
          )
        )

      case ("-b" | "--backward-only") :: rest =>
        parseArgs(
          rest,
          current.copy(formatter =
            current.formatter.copy(showForward = false, showBackward = true)
          )
        )

      case ("-j" | "--bytecode-names") :: rest =>
        parseArgs(
          rest,
          current.copy(formatter =
            current.formatter.copy(useBytecodeNames = true)
          )
        )

      case ("-v" | "--verbose") :: rest =>
        parseArgs(
          rest,
          current.copy(formatter =
            current.formatter.copy(showDescriptions = true)
          )
        )

      case ("-g" | "--include-generics") :: rest =>
        parseArgs(
          rest,
          current.copy(formatter =
            current.formatter.copy(showIncompatibleSignature = true)
          )
        )

      case filename :: rest if current.oldBinOpt.isEmpty =>
        parseArgs(rest, current.copy(oldBinOpt = Some(new File(filename))))
      case filename :: rest if current.newBinOpt.isEmpty =>
        parseArgs(rest, current.copy(newBinOpt = Some(new File(filename))))
      case wut :: _ =>
        throw new IllegalArgumentException(s"Unknown argument $wut")
    }

}
