organization := "org.scalasolutions"

name := "mima"

version = "1.0"

scalaVersion := "2.9.0"


mainClass in (Compile, packageBin) := Some("ssol.tools.mima.ui.MimaApp")
mainClass in (Compile, run) := Some("ssol.tools.mima.ui.MimaApp")