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
  versionScheme := Some("early-semver"),
  scalaVersion := scala212,
  resolvers ++= (if (isStaging) List(stagingResolver) else Nil),
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeOssSnapshots.head else Opts.resolver.sonatypeStaging),
))

def compilerOptions(scalaVersion: String): Seq[String] =
  Seq(
    "-feature",
    "-Wconf:cat=deprecation&msg=Stream|JavaConverters:s",
  ) ++
  (CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => Seq(
      "-Xlint",
      // these are too annoying when crossbuilding
      "-Wconf:cat=unused-nowarn:s",
    )
    case _ => Seq()
  }) ++
  (CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 12)) => Seq("-Xsource:3")
    case Some((2, 13)) => Seq("-Xsource:3-cross")
    case _ => Seq()
  })

// Useful to self-test releases
val stagingResolver = "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging"
def isStaging = sys.props.contains("mimabuild.staging")
commands += Command.command("testStaging") { state =>
  val prep = if (isStaging) Nil else List("reload")
  sys.props("mimabuild.staging") = "true"
  prep ::: "mimaReportBinaryIssues" :: state
}

// Keep in sync with TestCli
val scala212 = "2.12.19"
val scala213 = "2.13.13"
val scala3 = "3.3.3"

val root = project.in(file(".")).settings(
  name := "mima",
  crossScalaVersions := Nil,
  mimaFailOnNoPrevious := false,
  publish / skip := true,
)
aggregateProjects(core.jvm, core.native, sbtplugin, functionalTests)

val munit = Def.setting("org.scalameta" %%% "munit" % "1.0.0-M12")

val core = crossProject(JVMPlatform, NativePlatform).crossType(CrossType.Pure).settings(
  name := "mima-core",
  crossScalaVersions ++= Seq(scala213, scala3),
  scalacOptions ++= compilerOptions(scalaVersion.value),
  libraryDependencies += munit.value % Test,
  testFrameworks += new TestFramework("munit.Framework"),
  MimaSettings.mimaSettings,
  apiMappings ++= {
    // WORKAROUND https://github.com/scala/bug/issues/9311
    // from https://stackoverflow.com/a/31322970/463761
    sys.props.get("sun.boot.class.path").toList
      .flatMap(_.split(java.io.File.pathSeparator))
      .collectFirst { case str if str.endsWith(java.io.File.separator + "rt.jar") =>
        file(str) -> url("http://docs.oracle.com/javase/8/docs/api/index.html")
      }
      .toMap
  },

).nativeSettings(mimaPreviousArtifacts := Set.empty)

val sbtplugin = project.enablePlugins(SbtPlugin).dependsOn(core.jvm).settings(
  name := "sbt-mima-plugin",
  scalacOptions ++= compilerOptions(scalaVersion.value),
  // drop the previous value to drop running Test/compile
  scriptedDependencies := Def.task(()).dependsOn(publishLocal, core.jvm / publishLocal).value,
  scriptedLaunchOpts += s"-Dplugin.version=${version.value}",
  scriptedLaunchOpts += s"-Dsbt.boot.directory=${file(sys.props("user.home")) / ".sbt" / "boot"}",
  MimaSettings.mimaSettings,
)

val testFunctional = taskKey[Unit]("Run the functional test")
val functionalTests = Project("functional-tests", file("functional-tests"))
  .dependsOn(core.jvm)
  .configs(IntegrationTest)
  .settings(
    crossScalaVersions += scala213,
    libraryDependencies += "io.get-coursier" %% "coursier" % "2.1.9",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += munit.value,
    testFrameworks += new TestFramework("munit.Framework"),
    scalacOptions ++= compilerOptions(scalaVersion.value),
    //Test / run / fork := true,
    //Test / run / forkOptions := (Test / run / forkOptions).value.withWorkingDirectory((ThisBuild / baseDirectory).value),
    // Test / testOnly / watchTriggers += baseDirectory.value.toGlob / "src" / "test" / **,
    // ^ disabled b/c of "Build triggered by [..]/target/scala-2.12/v2-classes."
    testFunctional := (Test / test).value,
    Defaults.itSettings,
    Test / mainClass := Some("com.typesafe.tools.mima.lib.UnitTests"),
    IntegrationTest / testOptions += Tests.Argument(TestFrameworks.MUnit, "-b"), // disable buffering => immediate output
    mimaFailOnNoPrevious := false,
    publish / skip := true,
  )
