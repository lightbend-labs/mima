package ssol.tools.mima.core

import scala.tools.nsc.io.{ Path, Directory }
import scala.tools.nsc.util.JavaClassPath
import scala.util.Properties
import java.io.File

object Config {

  private var settings: Settings = _
  private var _classpath: JavaClassPath = _
  
  def info(str: String) = if (verbose) println(str)
  def debugLog(str: String) = if (debug) println(str)

  def inPlace = settings.mimaOutDir.isDefault
  def verbose = settings.verbose.value
  def debug = settings.debug.value
  def fixall = settings.fixall.value
  
  def error(msg: String) = System.err.println(msg)

  def baseClassPath: JavaClassPath = _classpath

  lazy val baseDefinitions = new Definitions(None, baseClassPath)

  def baseClassPath_=(cp: JavaClassPath) {
    _classpath = cp
  }

  def fatal(msg: String): Nothing = {
    error(msg)
    System.exit(-1)
    throw new Error()
  }

  lazy val outDir: Directory = {
    assert(!inPlace)
    val f = Path(settings.mimaOutDir.value).toDirectory
    if (!(f.isDirectory && f.canWrite)) fatal(f + " is not a writable directory")
    f
  }

  /** Creates a help message for a subset of options based on cond */
  def usageMsg(cmd: String): String =
    settings.visibleSettings.
      map(s => format(s.helpSyntax).padTo(21, ' ') + " " + s.helpDescription).
      toList.sorted.mkString("Usage: " + cmd + " <options>\nwhere possible options include:\n  ", "\n  ", "\n")

  def setup(s: Settings) {
    settings = s
  }

  def setup(cmd: String, args: Array[String], specificOptions: String*): List[String] =
    setup(cmd, args, _ => true, specificOptions: _*)

  def setup(cmd: String, args: Array[String], validate: List[String] => Boolean, specificOptions: String*): List[String] = {
    settings = new Settings(specificOptions: _*)
    val (_, resargs) = settings.processArguments(args.toList, true)
    _classpath = new PathResolver(settings).mimaResult
    if (settings.help.value) {
      println(usageMsg(cmd))
      System.exit(0)
    }
    if (validate(resargs)) resargs
    else fatal(usageMsg(cmd))
  }
}
