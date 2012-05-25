mainClass in (Compile, packageBin) := Some("com.typesafe.tools.mima.lib.ui.MimaLibApp")

mainClass in (Compile, sbt.Keys.run) := Some("com.typesafe.tools.mima.lib.ui.MimaLibApp")
