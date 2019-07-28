package com.typesafe.tools.mima.core

import scala.reflect.{ ClassTag, classTag }
import java.util.regex.Pattern

object ProblemFilters {

  private case class ExcludeByName[P <: ProblemRef: ClassTag](name: String) extends ProblemFilter {
    private[this] val pattern = Pattern.compile(name.split("\\*", -1).map(Pattern.quote).mkString(".*"))
    private[this] val cls     = classTag[P].runtimeClass

    override def apply(problem: Problem): Boolean = {
      !(cls.isAssignableFrom(problem.getClass) && pattern.matcher(problem.matchName.getOrElse("")).matches)
    }

    override def toString() = s"""ExcludeByName[${cls.getSimpleName}]("$name")"""
  }

  /** Creates an exclude filter by taking the type of the problem and the name of a match
   *  (such as the class or method name).
   */
  def exclude[P <: ProblemRef: ClassTag](name: String): ProblemFilter = ExcludeByName[P](name)

  /** Creates an exclude filter by taking the name of the problem and the name of a match
   *  (such as the class or method name).
   *
   *  The problemName is name of a class corresponding to a problem like `AbstractMethodProblem`.
   *
   *  @throws ClassNotFoundException if the class corresponding to the problem cannot be located
   */
  def exclude(problemName: String, name: String): ProblemFilter = {
    val problemClass: Class[_ <: ProblemRef] =
      Class.forName(s"com.typesafe.tools.mima.core.$problemName").asInstanceOf[Class[_ <: ProblemRef]]
    exclude(name)(ClassTag(problemClass))
  }

}
