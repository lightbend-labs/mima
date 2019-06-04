scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-unused:_,-imports",
)

addSbtPlugin("org.foundweekends" % "sbt-bintray"     % "0.5.5")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"         % "1.0.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"         % "1.1.2")
addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.3.0")
