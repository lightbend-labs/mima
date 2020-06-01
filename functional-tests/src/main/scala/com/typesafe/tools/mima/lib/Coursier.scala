package com.typesafe.tools.mima.lib

import coursier._

object Coursier {
  val scalaEA = mvn"https://scala-ci.typesafe.com/artifactory/scala-integration/"
  val scalaPR = mvn"https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/"

  def fetch(dep: Dependency) = Fetch().addRepositories(scalaEA, scalaPR).addDependencies(dep).run()
}
