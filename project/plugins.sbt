scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-unused:_,-imports",
)

// Useful to self-test releases
val stagingResolver = "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging"
def isStaging = sys.props.contains("mimabuild.staging")
resolvers ++= (if (isStaging) List(stagingResolver) else Nil)

addSbtPlugin("com.github.sbt"   % "sbt-dynver"      % "5.0.1")
addSbtPlugin("com.github.sbt" % "sbt-pgp"         % "2.2.1")
addSbtPlugin("com.typesafe"   % "sbt-mima-plugin" % "1.1.3")

addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.4")
