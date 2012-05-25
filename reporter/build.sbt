mainClass in (Compile, packageBin) := Some("com.typesafe.tools.mima.cli.Main")

mainClass in (Compile, sbt.Keys.run) := Some("com.typesafe.tools.mima.cli.Main")
