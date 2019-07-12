import mimabuild._

inThisBuild(Seq(
  organization := "com.typesafe",
  licenses := Seq("Apache License v2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("http://github.com/lightbend/mima")),
  developers := List(
    Developer("mdotta", "Mirco Dotta", "@dotta", url("https://github.com/dotta")),
    Developer("jsuereth", "Josh Suereth", "@jsuereth", url("https://github.com/jsuereth")),
  ),
  scmInfo := Some(ScmInfo(url("https://github.com/lightbend/mima"), "scm:git:git@github.com:lightbend/mima.git")),
  git.gitTagToVersionNumber := (tag => if (tag matches "[0.9]+\\..*") Some(tag) else None),
  git.useGitDescribe := true,
  scalaVersion := sys.props.getOrElse("mima.buildScalaVersion", "2.12.8"),
  scalacOptions := Seq("-feature", "-deprecation", "-Xlint"),
  crossScalaVersions := Nil,
))

val root = project.in(file(".")).disablePlugins(BintrayPlugin).enablePlugins(GitVersioning).settings(
  name := "mima",
  skip in publish := true,
)
aggregateProjects(core, sbtplugin, functionalTests)

val core = project.disablePlugins(BintrayPlugin).settings(
  name := "mima-core",
  crossScalaVersions := Seq("2.12.8", "2.13.0"),
  libraryDependencies += "com.typesafe" % "config" % "1.3.4",
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0-SNAP13" % Test,
  MimaSettings.mimaSettings,
)

val sbtplugin = project.enablePlugins(SbtPlugin).dependsOn(core).settings(
  name := "sbt-mima-plugin",
  crossScalaVersions := Seq("2.12.8"),
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
    crossScalaVersions := Seq("2.12.8", "2.13.0"),
    libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.1",
    mimaFailOnNoPrevious := false,
    skip in publish := true,
  )
