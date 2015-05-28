addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.9.1")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.5")

addSbtPlugin("com.eed3si9n" % "bintray-sbt" % "0.3.0-a1934a5457f882053b08cbdab5fd4eb3c2d1285d")
resolvers += Resolver.url("bintray-eed3si9n-sbt-plugins", url("https://dl.bintray.com/eed3si9n/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.4")

scalacOptions ++= Seq("-deprecation", "-language:_")
