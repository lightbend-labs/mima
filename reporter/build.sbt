mainClass in (Compile, packageBin) := Some("ssol.tools.mima.cli.Main")

mainClass in (Compile, sbt.Keys.run) := Some("ssol.tools.mima.cli.Main")
