package ssol.tools.mima

import scala.tools.nsc.io.{Path, Directory}
import scala.util.Properties

object Config {

  var settings: Settings = _

  def info(str: String) = if (settings.verbose.value) println(str)

  def inPlace = settings.mimaOutDir.isDefault

  def error(msg: String) = System.err.println(msg)

  def fatal(msg: String): Nothing = {
    error(msg)
    System.exit(-1)
    throw new Error()
  }

  lazy val outDir: Directory = {
    assert(!inPlace)
    val f = Path(settings.mimaOutDir.value).toDirectory
    if (!(f.isDirectory && f.canWrite)) fatal(f+" is not a writable directory")
    f
  }

  /** Creates a help message for a subset of options based on cond */
  def usageMsg(cmd: String): String =
    settings.visibleSettings .
      map(s => format(s.helpSyntax).padTo(21, ' ')+" "+s.helpDescription) .
      toList.sorted.mkString("Usage: "+cmd+" <options>\nwhere possible options include:\n  ", "\n  ", "\n")

  def setup(cmd: String, args: Array[String], validate: List[String] => Boolean, specificOptions: String*): List[String] = {
    settings = new Settings(specificOptions: _*)
    val (_, resargs) = settings.processArguments(args.toList, true)
    if (settings.help.value) {
      println(usageMsg(cmd))
      System.exit(0)
    }
    if (validate(resargs)) resargs
    else fatal(usageMsg(cmd))
  }
}
