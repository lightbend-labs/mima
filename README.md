# MiMa

MiMa (for "Migration Manager") is a tool for identifying [binary incompatibilities][] in Scala libraries.

[binary incompatibilities]: https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html

It's pronounced _MEE-ma_.

## What it is?

MiMa can report binary modifications that may
cause the JVM to throw a `java.lang.LinkageError` (or one of its subtypes,
like `AbstractMethodError`) at runtime. Linkage errors are usually the
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

MiMa's sbt plugin supports sbt 1.x only.  (Use v0.3.0 for sbt 0.13.x.)

To use it add the following to your `project/plugins.sbt` file:

```
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "<version>")
```

Add the following to your `build.sbt` file:

```
mimaPreviousArtifacts := Set("com.example" %% "my-library" % "<version>")
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

When MiMa reports a binary incompatibility that you consider acceptable, such as a change in an internal package,
you need to use the `mimaBinaryIssueFilters` setting to filter it out and get `mimaReportBinaryIssues` to
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

### IncompatibleSignatureProblem

Most MiMa checks (`DirectMissingMethod`, `IncompatibleResultType`,
`IncompatibleMethType`, etc) are against the "method descriptor", which
is the "raw" type signature, without any information about generic parameters.

The `IncompatibleSignature` check compares the `Signature`, which includes the
full signature including generic parameters. This can catch real
incompatibilities, but also sometimes triggers for a change in generics that
would not in fact cause problems at run time. Notably, it will warn when
updating your project to scala 2.12.9+ or 2.13.1+,
see [this issue](https://github.com/lightbend/mima/issues/423) for details.

You can opt-in to this check by setting:

```scala
import com.typesafe.tools.mima.plugin.MimaKeys._

ThisBuild / mimaReportSignatureProblems := true
```

### Annotation-based exclusions

The `mimaExcludeAnnotations` setting can be used to tell MiMa to
ignore classes, objects, and methods that have a particular
annotation.  Such an annotation might typically have "experimental" or
"internal" in the name.

The setting is a `Seq[String]` containing fully qualified annotation
names.

Example:

```scala
mimaExcludeAnnotations += "scala.annotation.experimental"
```

Caveat: `mimaExcludeAnnotations` is only implemented on Scala 3.

## Setting different mimaPreviousArtifacts

From time to time you may need to set `mimaPreviousArtifacts` according to some conditions.  For
instance, if you have already ported your project to Scala 2.13 and set it up for cross-building to Scala 2.13,
but still haven't cut a release, you may want to define `mimaPreviousArtifacts` according to the Scala version,
with something like:

```scala
mimaPreviousArtifacts := {
  if (CrossVersion.partialVersion(scalaVersion.value) == Some((2, 13)))
    Set.empty
  else
    Set("com.example" %% "my-library" % "1.2.3")
}
```

or perhaps using some of sbt 1.2's new API:

```scala
import sbt.librarymanagement.{ SemanticSelector, VersionNumber }

mimaPreviousArtifacts := {
  if (VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(">=2.13")))
    Set.empty
  else
    Set("com.example" %% "my-library" % "1.2.3")
}
```

## Make mimaReportBinaryIssues not fail

The setting `mimaFailOnNoPrevious` defaults to `true` and will make
`mimaReportBinaryIssues` fail if `mimaPreviousArtifacts` hasn't been set.

To make `mimaReportBinaryIssues` not fail you may want to do one of the following:

* set `mimaPreviousArtifacts` on all the projects that should be checking their binary compatibility
* avoid calling `mimaPreviousArtifacts` when binary compatibility checking isn't needed
* set `mimaFailOnNoPrevious := false` on specific projects that want to opt-out (alternatively `disablePlugins(MimaPlugin)`)
* set `ThisBuild / mimaFailOnNoPrevious := false`, which disables it build-wide, effectively reverting back to the previous behaviour

## Setting mimaPreviousArtifacts when name contains a "."

To refer to the project name in `mimaPreviousArtifacts`, use `moduleName` rather
than `name`, like

```scala
mimaPreviousArtifacts := Set(organization.value %% moduleName.value % "0.1.0")
```

Unlike `name`, `moduleName` escapes characters like `.`, and is the name
actually used by `publish` and `publishLocal` to publish your project. It's
also the value your users should use when adding your project to their
dependencies.
