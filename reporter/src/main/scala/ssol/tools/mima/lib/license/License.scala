package ssol.tools.mima.lib.license

import java.io.FileInputStream

object License {
  val license: String = {
    val url = getClass.getResourceAsStream("LICENSE")
    io.Source.fromInputStream(url).mkString
  }
}