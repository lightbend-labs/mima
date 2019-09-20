import mimabuild._

inThisBuild(Seq(
  organization := "com.typesafe",
  licenses := Seq("Apache License v2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("http://github.com/lightbend/mima")),
  developers := List(
    Developer("mdotta", "Mirco Dotta", "@dotta", url("https://github.com/dotta")),
    Developer("jsuereth", "Josh Suereth", "@jsuereth", url("https://github.com/jsuereth")),
    Developer("dwijnand", "Dale Wijnand", "@dwijnand", url("https://github.com/dwijnand")),
  ),
  scmInfo := Some(ScmInfo(url("https://github.com/lightbend/mima"), "scm:git:git@github.com:lightbend/mima.git")),
  dynverVTagPrefix := false,
  scalaVersion := scala212.value,
  scalaVersion := sys.props.getOrElse("mima.buildScalaVersion", scalaVersion.value),
  scalacOptions := Seq("-feature", "-deprecation", "-Xlint"),
//resolvers += stagingResolver,
))

// Useful to self-test releases
val stagingResolver = "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging"

val root = project.in(file(".")).disablePlugins(BintrayPlugin).settings(
  name := "mima",
  crossScalaVersions := Nil,
  mimaFailOnNoPrevious := false,
  skip in publish := true,
)
aggregateProjects(core, sbtplugin, functionalTests)

val core = project.disablePlugins(BintrayPlugin).settings(
  name := "mima-core",
  libraryDependencies ++= Seq(
    "org.scala-lang" %  "scala-compiler" % scalaVersion.value,
    "org.scalatest"  %% "scalatest"      % "3.0.8" % Test,
  ),
  MimaSettings.mimaSettings,
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),

)

val sbtplugin = project.enablePlugins(SbtPlugin).dependsOn(core).settings(
  name := "sbt-mima-plugin",
  crossScalaVersions := Seq(scala212.value),
  scriptedDependencies := scriptedDependencies.dependsOn(publishLocal in core).value,
  scriptedLaunchOpts += "-Dplugin.version=" + version.value,
  MimaSettings.mimaSettings,
  bintrayOrganization := Some("typesafe"),
  bintrayReleaseOnPublish := false,
)

val functionalTests = Project("functional-tests", file("functional-tests"))
  .dependsOn(core)
  .enablePlugins(TestsPlugin)
  .disablePlugins(BintrayPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe"           %  "config"                  % "1.3.4",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2",
    ),
    mimaFailOnNoPrevious := false,
    skip in publish := true,
  )
