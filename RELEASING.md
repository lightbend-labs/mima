# Releasing

Create a new [release tracking issue][] and get going.

[🚢 release tracking issue]: https://github.com/lightbend/mima/issues/new?template=release.md

## Prerequisites

* repo push rights
* publishing crendentials for Sonatype and Bintray, typically in `~/.sbt/1.0/credentials.sbt`:

```scala
    credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", <username>, <password>)
    credentials += Credentials("Bintray API Realm", "api.bintray.com", <username>, <password>)
```

(Make sure you're not using an ancient version of sbt-pgp in `~/.sbt/1.0/plugins`.)
