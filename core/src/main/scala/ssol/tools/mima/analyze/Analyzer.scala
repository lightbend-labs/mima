package ssol.tools.mima.analyze

import ssol.tools.mima.{Problem, ClassInfo}

/** Common trait for analyzers. 
 *  This class is NOT thread-safe. */
trait Analyzer {

  private val _problems = collection.mutable.ListBuffer.empty[Problem]
  
  protected def raise(problem: Problem) { _problems += problem }
  protected def raise(problem: Option[Problem]): Unit = problem match { 
    case Some(p) => raise(p) 
    case _ => () 
  }
  
  final def analyze(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    runAnalysis(oldclazz, newclazz)
    
    val problems = _problems.toList
    _problems.clear()
    
    problems
  }
  
  protected def runAnalysis(oldclazz: ClassInfo, newclazz: ClassInfo): Unit
}