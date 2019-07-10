# MiMa

MiMa (previously "The Migration Manager for Scala") is a tool for
diagnosing [binary incompatibilities][] for Scala libraries.

[binary incompatibilities]: https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html

## What it is?

MiMa can report binary modifications that may
lead the JVM throwing a ``java.lang.LinkageError`` (or one of its subtypes,
like ``AbstractMethodError``) at runtime. Linkage errors are usually the
consequence of modifications in classes/members signature.

MiMa compares all classfiles of two released libraries and reports all source
of incompatibilities that may lead to a linkage error. MiMa provides you, the
library maintainer, with a tool that can greatly automates and simplifies the
process of ensuring the release-to-release binary compatibility of your
libraries.

A key aspect of MiMa to be aware of is that it only looks for *syntactic binary
incompatibilities*. The semantic binary incompatibilities (such as adding or
removing a method invocation) are not considered. This is a pragmatic approach
as it is up to you, the library maintainer, to make sure that no semantic
changes have occurred between two binary compatible releases. If a semantic
change occurred, then you should make sure to provide this information as part
of the new release's change list.

In addition, it is worth mentioning that *binary compatibility does not imply
source compatibility*, i.e., some of the changes that are considered compatible
at the bytecode level may still break a codebase that depends on it.
Interestingly, this is not an issue intrinsic to the Scala language. In the
Java language binary compatibility does not imply source compatibility as well.
MiMa focuses on binary compatibility and currently provides no insight into
source compatibility.

## Usage

MiMa's sbt plugin is released for sbt 1.x only (use 0.3.0 for sbt 0.13).

To use it add the following to your `project/plugins.sbt` file:

```
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.4.0")
```

Add the following to your `build.sbt` file:

```
mimaPreviousArtifacts := Set("com.example" %% "my-library" % "1.2.3")
```

and run `mimaReportBinaryIssues` to see something like the following:

```
[info] Found 4 potential binary incompatibilities
[error]  * method rollbackTransactionResource()resource.Resource in object resource.Resource does not have a   correspondent in new version
[error]  * method now()scala.util.continuations.ControlContext in trait resource.ManagedResourceOperations does not    have a correspondent in old version
[error]  * abstract method now()scala.util.continuations.ControlContext in interface resource.ManagedResource does not have a correspondent in old version
[error]  * method rollbackTransactionResource()resource.Resource in trait resource.MediumPriorityResourceImplicits does not have a correspondent in new version
[error] {file:/home/jsuereth/project/personal/scala-arm/}scala-arm/*:mima-report-binary-issues: Binary compatibility check failed!
[error] Total time: 15 s, completed May 18, 2012 11:32:29 AM
```

## Filtering binary incompatibilities

When MiMa reports a binary incompatibility that you consider accetable, such as a change in an internal package,
you need to use the `mimaBinaryIssueFilters` setting to filter it out and get get `mimaReportBinaryIssues` to
pass, like so:

```scala
import com.typesafe.tools.mima.core._

mimaBinaryIssueFilters ++= Seq(
  ProblemFilters.exclude[MissingClassProblem]("com.example.mylibrary.internal.Foo"),
)
```

You may also use wildcards in the package and/or the top `Problem` parent type for such situations:

```scala
mimaBinaryIssueFilters ++= Seq(
  ProblemFilters.exclude[Problem]("com.example.mylibrary.internal.*"),
)
```

## Make mimaReportBinaryIssues not fail

The setting `mimaFailOnNoPrevious` (introduced in v0.4.0) defaults to `true` and will make
`mimaReportBinaryIssues` fail if `mimaPreviousArtifacts` is empty.

To make `mimaReportBinaryIssues` not fail you may what to do one of the following:

* set `mimaPreviousArtifacts` on all the projects that should be checking their binary compatibility
* set `mimaFailOnNoPrevious := false` on specific projects that want to opt-out (alternatively `disablePlugins(MimaPlugin)`)
* set `mimaFailOnNoPrevious in ThisBuild := false`, which disables it build-wide, effectively reverting back to the previous behaviour

You may want to redefine `mimaFailOnNoPrevious` in your build to be conditional on something else.  For
instance, if you have already ported your project to Scala 2.13 and set it up for cross-building to Scala 2.13,
but still haven't cut a release, you may want to redefine `mimaFailOnNoPrevious` according to the Scala version,
with something like:

```scala
// fail MiMa if no mimaPreviousArtifacts is set and NOT on Scala 2.13
mimaFailOnNoPrevious in ThisBuild := CrossVersion.partialVersion(scalaVersion.scala) != Some((2, 13))
```

or perhaps using some of sbt 1.2's new API:

```scala
import sbt.librarymanagement.{ SemanticSelector, VersionNumber }

// fail MiMa if no mimaPreviousArtifacts is set and on Scala <2.13
mimaFailOnNoPrevious in ThisBuild := VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector("<2.13"))
```
