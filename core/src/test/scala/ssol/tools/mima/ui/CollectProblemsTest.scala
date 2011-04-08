package ssol.tools.mima.ui

import ssol.tools.mima.{MiMaLib, Config}
import scala.io.Source

class CollectProblemsTest  {
  
  class TestFailed extends Exception
  
  def runTest(testName: String, oldJarPath: String, newJarPath: String, oraclePath: String) {
    Config.setup("scala ssol.tools.misco.MiMaLibUI <old-dir> <new-dir>", Array(oldJarPath, newJarPath), xs => true)
    val mima = new MiMaLib
    
    var problems = mima.collectProblems(oldJarPath, newJarPath).map(_.description)
    var expectedProblems = Source.fromFile(oraclePath).getLines.toList
    
    val unexpectedProblems = problems -- expectedProblems
    val unreportedProblem = expectedProblems -- problems
    
    var mess = new StringBuilder
    
    if(!unexpectedProblems.isEmpty)
      mess ++= "The following problems were not expected\n" + unexpectedProblems.mkString("\t- ", "\n","")
      
    if(!unreportedProblem.isEmpty)
      mess ++= "The following expected problems were not reported\n" + unreportedProblem.mkString("\t- ", "\n","")
    
    if(!mess.isEmpty) {
      println("[error] Test '" + testName + "' failed.\n" + mess.toString)
      
      throw new TestFailed
    }
  }
}