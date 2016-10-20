addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.9.1")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.5")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

scalacOptions ++= Seq("-feature", "-deprecation", "-Xfatal-warnings")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
