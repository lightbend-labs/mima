class ClassAnalyzer { // new version
  type Class = String
  lazy val superclasses: List[Class] = {
    // time consuming operation
    Thread.sleep(1000)  
    Nil
  }
}