ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "3.0.1"

val outputPath = settingKey[String]("Path of the output directory relative from the rootOutputDirectory.")

val `v1-0-0` =
  project.settings(
    name := "scala-3-mima",
    version := "1.0.0-SNAPSHOT"
  )

val `v1-0-1` =
  project.settings(
    name := "scala-3-mima",
    version := "1.0.1-SNAPSHOT",
    outputPath := outputPath.?.value.getOrElse("") + "101",
    mimaPreviousArtifacts := Set(organization.value %% name.value % "1.0.0-SNAPSHOT")
  )
