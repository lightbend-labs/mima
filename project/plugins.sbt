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

addSbtPlugin("com.dwijnand"   % "sbt-dynver"      % "4.1.1")
addSbtPlugin("com.github.sbt" % "sbt-pgp"         % "2.1.2")
addSbtPlugin("com.typesafe"   % "sbt-mima-plugin" % "1.1.0")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"    % "2.4.6")
