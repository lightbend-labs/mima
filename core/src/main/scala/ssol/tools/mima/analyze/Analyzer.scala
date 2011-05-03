package ssol.tools.mima.analyze

import ssol.tools.mima.Problem

/** Top trait for all Analyzer classes. Mind that in the current implementation 
 * analyzers are NOT thread-safe. */
trait Analyzer[T, S] extends Immutable {
  
  private lazy val _problems = collection.mutable.ListBuffer.empty[Problem]
  
  protected[analyze] def raise(problem: Problem) = _problems += problem
  
  final def analyze(left: T, right: S): Option[List[Problem]] = {
    runAnalysis(left, right)
    
    val problems = _problems.toList
    _problems.clear()
    if(problems.isEmpty) None else Some(problems)
  }
  
  protected def runAnalysis(left: T, right: S): Unit
}