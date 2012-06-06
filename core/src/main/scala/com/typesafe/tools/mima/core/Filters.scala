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
}
