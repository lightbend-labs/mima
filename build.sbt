import mimabuild._

inThisBuild(Seq(
  organization := "com.typesafe",
  licenses := Seq(License.Apache2),
  homepage := Some(url("http://github.com/lightbend-labs/mima")),
  developers := List(
    Developer("mdotta", "Mirco Dotta", "@dotta", url("https://github.com/dotta")),
    Developer("jsuereth", "Josh Suereth", "@jsuereth", url("https://github.com/jsuereth")),
    Developer("dwijnand", "Dale Wijnand", "@dwijnand", url("https://github.com/dwijnand")),
  ),
  scmInfo := Some(ScmInfo(url("https://github.com/lightbend-labs/mima"), "scm:git:git@github.com:lightbend-labs/mima.git")),
  dynverVTagPrefix := false,
  versionScheme := Some("early-semver"),
  scalaVersion := scala212,
  resolvers ++= (if (isStaging) List(stagingResolver) else Nil),
  publishTo := {
    val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
    if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
    else localStaging.value
  },
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
val scala212 = "2.12.20"
val scala213 = "2.13.16"
val scala3 = "3.3.6"
val scala3_7 = "3.7.3"

val root = project.in(file(".")).settings(
  name := "mima",
  crossScalaVersions := Nil,
  mimaFailOnNoPrevious := false,
  publish / skip := true,
)
aggregateProjects(core.jvm, core.native, cli.jvm, sbtplugin, functionalTests)

val munit = Def.setting("org.scalameta" %%% "munit" % "1.1.1")

val core = crossProject(JVMPlatform, NativePlatform).crossType(CrossType.Pure).settings(
  name := "mima-core",
  crossScalaVersions ++= Seq(scala213, scala3),
  scalacOptions ++= compilerOptions(scalaVersion.value),
  libraryDependencies += munit.value % Test,
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

val cli = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(
    name := "mima-cli",
    crossScalaVersions ++= Seq(scala3),
    scalacOptions ++= compilerOptions(scalaVersion.value),
    libraryDependencies += munit.value % Test,
    MimaSettings.mimaSettings,
    // cli has no previous release,
    // but also we don't care about its binary compatibility as it's meant to be used standalone
    mimaPreviousArtifacts := Set.empty
  )
  .dependsOn(core)

val sbtplugin = project.enablePlugins(SbtPlugin).dependsOn(core.jvm).settings(
  name := "sbt-mima-plugin",
  crossScalaVersions ++= Seq(scala3_7),
  (pluginCrossBuild / sbtVersion) := {
    scalaBinaryVersion.value match {
      case "2.12" => "1.5.8"
      case _      => "2.0.0-RC3"
    }
  },
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
    libraryDependencies += "io.get-coursier" %% "coursier" % "2.1.24",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += munit.value,
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
