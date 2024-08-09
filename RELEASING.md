# Releasing

See the [prerequisites](#prerequisites) if this is your first release.

## Step 0: Create a new release tracking

1. Click ["new issue"][issues/new];
2. Opening the raw [RELEASING.md][] content; and
3. Copying the following release checklist, up to "You are done!", into the new issue and create it.

## Release checklist

* [ ] [Check CI][ci] passes against the latest Scala versions.
* [ ] [Draft a new release][releases/new] for the release, allowing GitHub to generate draft release notes automatically.
* [ ] Run `git tag -a -s 1.x.y -m 'Version 1.x.y'`. The name of the tag should NOT have a `v` prefix.
* [ ] In sbt run `reload` and `show version` to verify the version.
* [ ] In sbt run `clean`, particularly if you've recently bumped `scalaVersion`.
* [ ] In sbt run `publishSigned`. You should start seeing "published mima-.. to https://oss.sonatype.org/service/local/staging/deploy/maven2/..".
* [ ] In sbt run `++2.13` `coreJVM/publishSigned` `coreNative/publishSigned`
* [ ] In sbt run `++3.3` `coreJVM/publishSigned` `coreNative/publishSigned`
* [ ] [Find and close][sonatype/staging-repos] your staging repository.  (See Sonatype's [Releasing the Deployment][sonatype/guide] guide.)
* [ ] Switch to a branch (e.g. `git checkout -b release-1.x.y`)
* [ ] In `project/plugins.sbt` update `sbt-mima-plugin`.
* [ ] In `project/MimaSettings.scala` update `mimaPreviousVersion` & clear out `mimaBinaryIssueFilters`.
* [ ] In sbt run `testStaging` **WITHOUT** `reload`ing first (`testStaging` adds the staging resolvers & runs `reload`).
* [ ] Update the version numbers in `RELEASING.md`
* [ ] Run `git add -p` and `git commit -am 'Update sbt-mima-plugin to 1.x.y'` and PR it (`gh pr create` or `hub pull-request`). The PR won't pass CI until the release is available on Maven Central. You may poll [repo1 directly][repo1/list] (note the trailing slash in the URL).
* [ ] [Find and release][sonatype/staging-repos] your staging repository.
* [ ] Switch back to the main branch and run `git push --follow-tags` to push the tag.
* [ ] [Find and merge][prs/list] your update PR.
* [ ] [Find and hit "Publish Release"][releases/list] on the draft GitHub release.

[compare/view]:    https://github.com/lightbend-labs/mima/compare/1.1.4...main
[issues/new]:      https://github.com/lightbend-labs/mima/issues/new
[prs/list]:        https://github.com/lightbend-labs/mima/pulls
[releases/list]:   https://github.com/lightbend-labs/mima/releases
[releases/new]:    https://github.com/lightbend-labs/mima/releases/new

[RELEASING.md]: https://raw.githubusercontent.com/lightbend-labs/mima/main/RELEASING.md
[repo1/list]: https://repo1.maven.org/maven2/com/typesafe/mima-core_2.12/1.1.4/
[sonatype/guide]: https://central.sonatype.org/pages/releasing-the-deployment.html
[sonatype/staging-repos]: https://oss.sonatype.org/#stagingRepositories
[ci]: https://github.com/lightbend-labs/mima/actions/workflows/ci.yml

You are done!

## Prerequisites

* repo push rights
* publishing credentials for Sonatype, typically in `~/.sbt/1.0/credentials.sbt`:

```scala
    credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", <username>, <password>)
```

(Make sure you're not using an ancient version of sbt-pgp in `~/.sbt/1.0/plugins`.)
