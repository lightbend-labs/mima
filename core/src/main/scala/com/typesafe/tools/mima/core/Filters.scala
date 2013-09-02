package com.typesafe.tools.mima.core
import scala.reflect.ClassTag

object ProblemFilters {

  private case class ExcludeByName[P <: ProblemRef: ClassTag](name: String) extends ProblemFilter {
    override def apply(problem: Problem): Boolean = {
      !(implicitly[ClassTag[P]].runtimeClass.isAssignableFrom(problem.getClass) &&
        Some(name) == problem.matchName)
    }

    override def toString(): String = """ExcludeByName[%s]("%s")""".format(implicitly[ClassTag[P]].runtimeClass.getSimpleName, name)
  }

  def exclude[P <: ProblemRef: ClassTag](name: String): ProblemFilter = {
    ExcludeByName[P](name)
  }

  /**
   *  Creates exclude filter by taking name of a problem and name of a match (e.g. class/method name).
   *
   *  The problemName is name of a class corresponding to a problem like `AbstractMethodProblem`.
   *
   *  @throws ClassNotFoundException if the class corresponding to the problem cannot be located
   */
  def exclude(problemName: String, name: String): ProblemFilter = {
    val problemClass: Class[_ <: ProblemRef] = Class.forName("com.typesafe.tools.mima.core." + problemName).asInstanceOf[Class[_ <: ProblemRef]]
    exclude(name)(ClassTag(problemClass))
  }

  private case class ExcludeByPackage(excludedPackageName: String) extends ProblemFilter {
    def apply(problem: Problem): Boolean = {
      // careful to avoid excluding "com.foobar" with an exclusion "com.foo"
      !problem.matchName.getOrElse("").startsWith(excludedPackageName + ".")
    }
  }

  def excludePackage(packageName: String): ProblemFilter = new ExcludeByPackage(packageName)
}
