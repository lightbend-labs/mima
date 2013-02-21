package com.typesafe.tools.mima.core

object ProblemFilters {

  private case class ExcludeByName[P <: ProblemRef: ClassManifest](name: String) extends ProblemFilter {
    override def apply(problem: Problem): Boolean = {
      !(implicitly[ClassManifest[P]].erasure.isAssignableFrom(problem.getClass) &&
        Some(name) == problem.matchName)
    }

    override def toString(): String = """ExcludeByName[%s]("%s")""".format(implicitly[ClassManifest[P]].erasure.getSimpleName, name)
  }

  def exclude[P <: ProblemRef: ClassManifest](name: String): ProblemFilter = {
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
    exclude(name)(ClassManifest.fromClass(problemClass))
  }
}
