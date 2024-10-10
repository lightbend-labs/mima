organization := "com.typesafe"
name := "pom-only-project"
version := "1.1.0"
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % "1.0.0")
scalaVersion := "2.13.15"

// this is an arbitrary dependency, but one that's known to cause issues in POM-only projects
// see https://github.com/lightbend-labs/mima/issues/768 for more context
libraryDependencies += "com.twitter" %% "util-core" % "22.7.0"
