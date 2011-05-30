package license

object License {
  val license: String = {
    val url = getClass.getResourceAsStream("LICENSE")
    io.Source.fromInputStream(url).mkString
  }
}