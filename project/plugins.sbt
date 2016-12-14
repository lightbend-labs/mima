addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

// we cannot use -Xfatal-warnings here since switching from Build.scala
// to build.sbt is blocked by sbt/sbt#2532, so we get a deprecation
// warning
scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

libraryDependencies += "com.typesafe" % "config" % "1.3.0"
