package com.typesafe.tools.mima.lib

import java.io.File

import coursier._

object Coursier {
  val scalaEA           = mvn"https://scala-ci.typesafe.com/artifactory/scala-integration/"
  val scalaPR           = mvn"https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/"
  val sbtPluginReleases = Repositories.sbtPlugin("releases")

  def fetch(dep: Dependency): Seq[File] =
    Fetch().addRepositories(scalaEA, scalaPR, sbtPluginReleases).addDependencies(dep).run()
}
