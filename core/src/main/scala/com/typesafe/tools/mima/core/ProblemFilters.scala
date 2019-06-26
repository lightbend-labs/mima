package com.typesafe.tools.mima.core
import scala.reflect.ClassTag
import java.util.regex.Pattern

object ProblemFilters {

  private case class ExcludeByName[P <: ProblemRef: ClassTag](name: String) extends ProblemFilter {
    private[this] val pattern = Pattern.compile(name.split("\\*", -1).map(Pattern.quote).mkString(".*"))
    override def apply(problem: Problem): Boolean = {
      !(implicitly[ClassTag[P]].runtimeClass.isAssignableFrom(problem.getClass) &&
        pattern.matcher(problem.matchName.getOrElse("")).matches)
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

  @deprecated("Replace with ProblemFilters.exclude[Problem](\"my.package.*\")", "0.1.15")
  def excludePackage(packageName: String): ProblemFilter = {
    exclude[Problem](packageName + ".*")
  }
}
