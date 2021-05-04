ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "3.0.0-RC3"

val `v1-0-0` =
  project.settings(
    name := "scala-3-mima",
    version := "1.0.0-SNAPSHOT"
  )

val `v1-0-1` =
  project.settings(
    name := "scala-3-mima",
    version := "1.0.1-SNAPSHOT",
    mimaPreviousArtifacts := Set(organization.value %% name.value % "1.0.0-SNAPSHOT")
  )
