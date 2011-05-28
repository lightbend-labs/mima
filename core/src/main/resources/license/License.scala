package license

object License {
  val license: String = {
    val url = getClass.getResource("LICENSE")
    io.Source.fromFile(url.toURI).mkString
  }
}