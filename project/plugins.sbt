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

addSbtPlugin("com.dwijnand"      % "sbt-dynver"      % "4.1.1")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"         % "2.0.2")
addSbtPlugin("com.typesafe"      % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("com.dwijnand"      % "sbt-travisci"    % "1.2.0")
