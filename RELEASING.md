# Releasing

Step 🚢: Create a [new issue][issues/new] with the following checklist to the track the release!

See the [prerequisites](#prerequisites) if this is your first release.

* [ ] [Check Travis CI][travis-ci] passes against the latest Scala versions.
* [ ] [Create a new milestone][milestones/new] for the release you are about to perform, if one [doesn't exist][milestones/list].
* [ ] [Create a new milestone][milestones/new] for the release after this one.
* [ ] [Draft a new release][releases/new] for the release, by [comparing changes][compare/view].
* [ ] Bump the version in the `README.md` and `git commit -m 'Release 0.x.y`
* [ ] Run `clean` in sbt, particularly if you've recently bumped `scalaVersion`.
* [ ] Run `git tag -a -s 0.x.y -m 'Version 0.x.y` and `git push --follow-tags`. The name of the tag should NOT have a 'v' prefix. Run `reload` and `show version` in sbt to verify the version.
* [ ] Run `^publishSigned` in sbt. You should start seeing "published mima-.. to https://oss.sonatype.org/service/local/staging/deploy/maven2/.."
* [ ] [Find and close][sonatype/staging-repos] your stating repository.  (See Sonatype's [Releasing the Deployment][sonatype/guide] guide.)
* [ ] Test the release by adding `resolvers ++= Seq("Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging", Resolver.bintrayRepo("typesafe", "sbt-plugins"))` to a project
* [ ] [Find and release][sonatype/staging-repos] your stating repository.
* [ ] Run `sbtplugin/bintrayRelease` in sbt (or use the [Web UI](https://bintray.com/typesafe/sbt-plugins/sbt-mima-plugin/view) to publish the sbt plugin artifacts.
* [ ] [Close][milestones/list] the milestone
* [ ] Wait for the artifacts to show up on Maven Central, either by:
  * successfully resolving the sbt plugin jar and dependent jars with sbt
  * successfully resolving the virtual directory: <https://repo1.maven.org/maven2/com/typesafe/mima-core_2.12/0.5.0/> (note the trailing slash)
* [ ] [Hit "Publish Release"][releases/list] on GitHub.

You are done!

[compare/view]:    https://github.com/lightbend/mima/compare/0.4.0...master
[issues/new]:      https://github.com/lightbend/mima/issues/new
[milestones/list]: https://github.com/lightbend/mima/milestones?direction=asc
[milestones/new]:  https://github.com/lightbend/mima/milestones/new
[releases/list]:   https://github.com/lightbend/mima/releases
[releases/new]:    https://github.com/lightbend/mima/releases/new

[sonatype/guide]: https://central.sonatype.org/pages/releasing-the-deployment.html
[sonatype/staging-repos]: https://oss.sonatype.org/#stagingRepositories
[travis-ci]: https://travis-ci.org/lightbend/mima

## Prerequisites

* repo push rights
* publishing crendentials for Sonatype and Bintray, typically in `~/.sbt/1.0/credentials.sbt`:

```scala
    credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", <username>, <password>)
    credentials += Credentials("Bintray API Realm", "api.bintray.com", <username>, <password>)
```

(Make sure you're not using an ancient version of sbt-pgp in `~/.sbt/1.0/plugins`.)
