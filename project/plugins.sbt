scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-unused:_,-imports",
)

addSbtPlugin("com.github.sbt"   % "sbt-dynver"      % "5.1.1")
addSbtPlugin("com.github.sbt" % "sbt-pgp"         % "2.3.1")
addSbtPlugin("com.typesafe"   % "sbt-mima-plugin" % "1.1.4")

addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.9")
