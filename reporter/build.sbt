name := "mima-lib"

libraryDependencies ++= Seq("org.scala-lang" % "scala-swing" % "2.9.0" % "compile")

mainClass in (Compile, packageBin) := Some("ssol.tools.mima.lib.ui.MimaApp")

mainClass in (Compile, run) := Some("ssol.tools.mima.lib.ui.MimaApp")