scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-unused:_,-imports",
)

// Useful to self-test releases
//resolvers ++= stagingResolvers
val stagingResolvers = Seq(
  "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging",
  Resolver.bintrayIvyRepo("typesafe", "sbt-plugins"),
)

addSbtPlugin("org.foundweekends" % "sbt-bintray"     % "0.5.5")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"         % "1.0.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"         % "1.1.2")
addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.5.0")
