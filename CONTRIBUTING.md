# Contributing to MiMa

## CLA

If you'd like to contribute to the MiMa project, please sign the [contributor's licensing agreement](http://www.lightbend.com/contribute/cla).

## Modules

MiMa is split into several modules:

- mima-core: classes that are used for detection and reporting
- sbt-mima-plugin: the sbt plugin to run MiMa within an sbt build

## Building

Using [sbt][sbt]:

      $ sbt compile

[sbt]: http://www.scala-sbt.org/

This will recompile all MiMa's modules.

## Functional tests

The directory 'functional-tests' contains several functional tests exercising MiMa. All tests are executed as part of the build, therefore when running

    $ sbt testFunctional

if one (or more) test fails, the build is stopped and no jar will be produced.

To add a new functional test to the suite, create a new folder within the 'functional-tests' directory with the following structure:

    functional-tests
        |
        | --> <your-new-test-folder> (folder for your new test)
            |
            |-----> problems.txt (the expected list of reported errors - 1 line per error)
            |-----> v1 (folder containing sources @ version 1)
            |-----> v2 (folder containing sources @ version 2)

After doing that, `reload` if you are in an `sbt` shell session (if that makes no sense to you, it means you are fine and you can run the test as usual).

Tests within the `functional-tests` folder should always pass.

Note: The `problems.txt` is the test oracle. Expected errors are declared using the MiMa's reporting output (i.e., the output of the tool and the expected errors should match perfectly). Admittedly, this coupling is an issue since the testing framework is highly coupled with the tool output used to report errors to the user. We should improve this and make the two independent. Until then, mind that by changing the output of the tool you will likely have to update some of the test oracles (i.e., problems.txt file). When running tests against Scala 2.12 or higher, `problems-2.12.txt` is preferred over `problems.txt` if the former exists.

## General Workflow

This is the process for committing code into master. There are of course exceptions to these rules, for example minor changes to comments and documentation, fixing a broken build etc.

1. Make sure you have signed the [Lightbend CLA](http://www.lightbend.com/contribute/cla). If not, please sign it online.
2. Before starting work on a feature or a fix, make sure that there is a ticket for your work in the project's issue tracker. If not, create it first.
3. Fork the project and perform your work in a Git branch.
4. When the feature or fix is completed you should open a [Pull Request](https://help.github.com/articles/using-pull-requests) on GitHub.
5. The Pull Request should be reviewed by other maintainers (as many as feasible/practical). Note that the maintainers can consist of outside contributors, both within and outside Lightbend. Outside contributors are encouraged to participate in the review process, it is not a closed process.
6. After the review you should fix the issues as needed, **pushing the changes as additional commits**, iterating until the reviewers give their thumbs up.
7. Once the code has passed review, it’s ok to amend commits as needed (see the ‘Creating Commits And Writing Commit Messages’ section below).
8. The Pull Request can be merged into the master branch.
9. If the code change needs to be applied to other branches as well, create pull requests against those branches with the change rebased onto the respective branches and await successful verification by the continuous integration infrastructure; then merge those pull requests.
10. Once everything is said and done, associate the ticket with the “earliest” release branch (i.e. if back-ported so that it will be in release x.y.z, find the relevant milestone for that release) and close it.

## Pull Request Requirements

For a Pull Request to be considered at all it has to meet these requirements:

1. Live up to the current code standard:
   - Not violate [DRY](http://programmer.97things.oreilly.com/wiki/index.php/Don%27t_Repeat_Yourself).
   - [Boy Scout Rule](http://programmer.97things.oreilly.com/wiki/index.php/The_Boy_Scout_Rule) needs to have been applied.
2. Regardless of whether the code introduces new features or fixes bugs or regressions, it must have comprehensive tests.
3. The code must be well documented.
4. User documentation should be provided for all new features.
5. Rebase your branch on the latest master if it can’t be cleanly merged.
6. Pull Request validation passes. What Pull Request validation does is make sure that the current master branch will still compile fine after the currently in-review Pull Request is merged.

If these requirements are not met then the code should **not** be merged into master, or even reviewed - regardless of how good or important it is.

## Creating Commits And Writing Commit Messages

Follow these guidelines when creating public commits and writing commit messages.

1. If your work spans multiple local commits (for example; if you do safe point commits while working in a feature branch or work in a branch for long time doing merges/rebases etc.) then please do not commit it all but rewrite the history by squashing the commits into as few as necessary. Every commit should be able to be used in isolation, cherry picked etc.
2. First line should be a descriptive sentence what the commit is doing. It should be possible to fully understand what the commit does by just reading this single line. It is **not ok** to only list the ticket number, type "minor fix" or similar. If the commit is a small fix, then go to 4. Otherwise, keep reading.
3. Following the single line description should be a blank line followed by a detailed description of the problem the commit solves and justify your solution. For more info, read this article: [Writing good commit messages](https://github.com/erlang/otp/wiki/Writing-good-commit-messages).
4. Add keywords for your commit (depending on the degree of automation we reach, the list may change over time):
    * ``Review by @gituser`` - if you want to notify someone on the team. The others can, and are encouraged to participate.
    * ``Fix #ticket`` - if the commit fixes a ticket (or``Fix #ticket1``, ..., ``Fix #ticketN``, if it fixes several tickets).

Example:

    Corrected semantic highlighting for methods

    Details 1

    Details 2

    Details 3

    Fix #2731, Fix #2732, Re #2733

## Releasing

### Prerequisites

Make sure that you have:

* push rights to this project,

* credentials to deploy artifacts on both ``oss.sonatype.org`` and ``bintray.com/typesafe/sbt-plugins``. This usually means that in your local ``~/.sbt`` folder you have a ``.sbt`` file (e.g., ``user.sbt``, ``credentials.sbt``) that contains something like the following:
```bash
    credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", <username>, <password>)

    credentials += Credentials("Bintray API Realm", "api.bintray.com", <username>, <password>)
```

(Make sure you're not using an ancient version of sbt-pgp in `~/.sbt/1.0/plugins`.)

### Steps

Create a new issue with the following checklist and get going.

- [ ] A milestone exists for the release you are about to perform. If it doesn't create one.
- [ ] The milestone due date is the day of the release.
- [ ] Travis passes against latest Scala versions (past example: https://github.com/lightbend/mima/pull/164)
- [ ] All tickets fixed in the current development cycle are associated to the current milestone.
- [ ] Bump the version in the `README.md` and `git commit`.
- [ ] Run `clean`. (particularly if you've recently bumped `scalaVersion` - see [#191](https://github.com/lightbend/mima/issues/191))
- [ ] Create an annotated, signed git tag (`git tag -a -s`) and push it. The name of the tag should follow the format of previous tags (not 'v' prefix). `reload` and verify that `show version` in sbt picks up the version number from the tag.
- [ ] Run `^publishSigned`. You should start seeing "published mima-.. to https://oss.sonatype.org/service/local/staging/deploy/maven2/.."
- [ ] Login on [Sonatype](https://oss.sonatype.org/) and follow [this guide](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide#SonatypeOSSMavenRepositoryUsageGuide-8a.ReleaseIt) to release the staged MiMa artifacts.
- [ ] Login to [Bintray](https://bintray.com/typesafe/sbt-plugins/sbt-mima-plugin/view) and publish sbt plugin artifacts. (or use `sbtplugin/bintrayRelease`)
- [ ] Wait for artifacts to show up on Maven Central
  - [ ] sbt plugin jar and dependent jars resolvable by sbt
  - [ ] virtual directory appears: https://repo1.maven.org/maven2/com/typesafe/mima-core_2.12/0.3.0/
- [ ] Close milestone
- [ ] Add release notes to tag
- [ ] Hit "Publish Release" on GitHub
- [ ] Announce the release in the [Announce category of Scala Users](https://users.scala-lang.org/c/announce)
    (e.g https://users.scala-lang.org/t/announcing-mima-migration-manager-0-3-0/2782)

You are done!
