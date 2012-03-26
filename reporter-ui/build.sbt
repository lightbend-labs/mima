mainClass in (Compile, packageBin) := Some("ssol.tools.mima.lib.ui.MimaLibApp")

mainClass in (Compile, sbt.Keys.run) := Some("ssol.tools.mima.lib.ui.MimaLibApp")
