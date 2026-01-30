# Releasing

## Step 0: Create a new release tracking

1. Click ["new issue"][issues/new];
2. Opening the raw [RELEASING.md][] content; and
3. Copying the following release checklist, up to "You are done!", into the new issue and create it.

## Release checklist

* [ ] [Check CI][wf/ci] passes against the latest Scala versions.
* [ ] Run `git tag -a -s v1.x.y -m 'Version 1.x.y'`. The name of the tag MUST have a `v` prefix (required by sbt-ci-release).
* [ ] In sbt run `reload` and `show version` to verify the version.
* [ ] Push the tag: `git push origin v1.x.y`. This will trigger the automated release workflow on GitHub Actions.
* [ ] Monitor the [release workflow][wf/release]
* [ ] Switch to a branch (e.g. `git checkout -b release-1.x.y`)
* [ ] In `project/plugins.sbt` update `sbt-mima-plugin`.
* [ ] In `project/MimaSettings.scala` update `mimaPreviousVersion` & clear out `mimaBinaryIssueFilters`.
* [ ] Run `git add -p` and `git commit -am 'Update sbt-mima-plugin to 1.x.y'` and PR it (`gh pr create` or `hub pull-request`). The PR won't pass CI until the release is available on Maven Central. You may poll [repo1 directly][repo1/list] (note the trailing slash in the URL).
* [ ] [Find and merge][prs/list] your update PR.
* [ ] [Create a new release][releases/new] for the release, allowing GitHub to generate draft release notes automatically.

[issues/new]:    https://github.com/lightbend-labs/mima/issues/new
[prs/list]:      https://github.com/lightbend-labs/mima/pulls
[releases/new]:  https://github.com/lightbend-labs/mima/releases/new
[wf/release]:    https://github.com/lightbend-labs/mima/actions/workflows/release.yml
[RELEASING.md]:  https://raw.githubusercontent.com/lightbend-labs/mima/main/RELEASING.md
[repo1/list]:    https://repo1.maven.org/maven2/com/typesafe/mima-core_2.12/
[wf/ci]:         https://github.com/lightbend-labs/mima/actions/workflows/ci.yml

You are done!
