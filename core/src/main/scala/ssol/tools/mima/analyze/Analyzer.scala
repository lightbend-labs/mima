package ssol.tools.mima.analyze

import ssol.tools.mima.Problem

/** Top trait for all Analyzer classes. Analyzers should be immutable, 
 *  this is why the trait is marked as @Immutable. */
trait Analyzer[T, S] extends Immutable {
  
  protected def reporterFactory: ReporterFactory = new DefaultReporterFactory
  
  final def analyze(left: T, right: S): Option[List[Problem]] = {
    val reporter = reporterFactory.createReporter
    analyze(reporter)(left, right)
    
    val problems = reporter.problems
    if(problems.isEmpty) None else Some(problems)
  }
  
  protected def analyze(reporter: Reporter)(left: T, right: S): Unit
}