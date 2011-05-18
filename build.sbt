organization := "org.scalasolutions"

name := "mima"

version = "0.0.1"

scalaVersion := "2.9.0"

// Use the name and version to define the jar name.
val qualifier = "alpha"
jarName <<= (name, version) {
	(name: String, version: String) => name + "-" + version + "-" + qualifier + ".jar"
}


mainClass in (Compile, packageBin) := Some("ssol.tools.mima.ui.MimaApp")
mainClass in (Compile, run) := Some("ssol.tools.mima.ui.MimaApp")