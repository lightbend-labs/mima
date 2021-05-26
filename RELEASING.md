# Releasing

See the [prerequisites](#prerequisites) if this is your first release.

## Step 0: Create a new release tracking

1. Click ["new issue"][issues/new];
2. Opening the raw [RELEASING.md][] content; and
3. Copying the following release checklist, up to "You are done!", into the new issue and create it.

## Release checklist

* [ ] [Check Travis CI][travis-ci] passes against the latest Scala versions.
* [ ] [Create a new milestone][milestones/new] for the release you are about to perform, if one [doesn't exist][milestones/list].
* [ ] [Draft a new release][releases/new] for the release, by [comparing changes][compare/view].
* [ ] Bump the version in the `README.md` and `git commit -am 'Release 0.x.y`.
* [ ] Run `git tag -a -s 0.x.y -m 'Version 0.x.y`. The name of the tag should NOT have a 'v' prefix.
* [ ] In sbt run `reload` and `show version` to verify the version.
* [ ] In sbt run `clean`, particularly if you've recently bumped `scalaVersion`.
* [ ] In sbt run `publishSigned`. You should start seeing "published mima-.. to https://oss.sonatype.org/service/local/staging/deploy/maven2/..".
* [ ] In sbt run `++2.13.6 core/publishSigned`, using the right 2.13 version.
* [ ] [Find and close][sonatype/staging-repos] your staging repository.  (See Sonatype's [Releasing the Deployment][sonatype/guide] guide.)
* [ ] Switch to a branch (e.g. `git checkout -b release`)
* [ ] In `plugins.sbt` update `sbt-mima-plugins`.
* [ ] In `project/MimaSettings.scala` update `mimaPreviousVersion` & clear out `mimaBinaryIssueFilters`.
* [ ] In sbt run `testStaging` **WITHOUT** `reload`ing first (`testStaging` adds the staging resolvers & runs `reload`).
* [ ] Run `git commit -am 'Update sbt-mima-plugin to 0.x.y` and PR it (`hub pull-request`).
* [ ] [Find and release][sonatype/staging-repos] your staging repository.
* [ ] [Close][milestones/list] the milestone.
* [ ] Switch back to the main branch and run `git push --follow-tags` to push the tag.
* [ ] [Find and merge][prs/list] your update PR. You may poll [repo1 directly][repo1/list] (note the trailing slash in the URL).
* [ ] [Find and hit "Publish Release"][releases/list] on the draft GitHub release.

[compare/view]:    https://github.com/lightbend/mima/compare/0.9.0...main
[issues/new]:      https://github.com/lightbend/mima/issues/new
[milestones/list]: https://github.com/lightbend/mima/milestones?direction=asc
[milestones/new]:  https://github.com/lightbend/mima/milestones/new
[prs/list]:        https://github.com/lightbend/mima/pulls
[releases/list]:   https://github.com/lightbend/mima/releases
[releases/new]:    https://github.com/lightbend/mima/releases/new

[RELEASING.md]: https://raw.githubusercontent.com/lightbend/mima/main/RELEASING.md
[repo1/list]: https://repo1.maven.org/maven2/com/typesafe/mima-core_2.12/0.5.0/
[sonatype/guide]: https://central.sonatype.org/pages/releasing-the-deployment.html
[sonatype/staging-repos]: https://oss.sonatype.org/#stagingRepositories
[travis-ci]: https://travis-ci.com/github/lightbend/mima

You are done!

## Prerequisites

* repo push rights
* publishing crendentials for Sonatype, typically in `~/.sbt/1.0/credentials.sbt`:

```scala
    credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", <username>, <password>)
```

(Make sure you're not using an ancient version of sbt-pgp in `~/.sbt/1.0/plugins`.)
