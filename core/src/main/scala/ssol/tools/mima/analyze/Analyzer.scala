package ssol.tools.mima.analyze

import ssol.tools.mima.{Problem, ClassInfo}

/** Common trait for analyzers. 
 *  This class is NOT thread-safe. */
trait Analyzer {

  private val _problems = collection.mutable.ListBuffer.empty[Problem]
  
  protected def raise(problem: Problem) { _problems += problem }
  
  final def analyze(): List[Problem] = {
    runAnalysis()
    
    val problems = _problems.toList
    _problems.clear()
    
    problems
  }
  
  protected def runAnalysis(): Unit
}