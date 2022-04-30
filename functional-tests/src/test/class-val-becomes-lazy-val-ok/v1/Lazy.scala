class ClassAnalyzer { // old version
  type Class = String
  val superclasses: List[Class] = {
    // time consuming operation
    Thread.sleep(1000)
    Nil
  }
}
