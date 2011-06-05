organization := "org.scalasolutions"

name := "mima"

version := "0.1"

scalaVersion := "2.9.0"

// Use the name and version to define the jar name.
// XXX: Why this doesn't work?
//jarName <<= (name, version) {
//	(name: String, version: String) => name + "-" + version + ".jar"
//}