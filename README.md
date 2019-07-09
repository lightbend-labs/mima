# MiMa

MiMa (previously "The Migration Manager for Scala") is a tool for
diagnosing [binary incompatibilities](https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html) for Scala libraries.

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

The current version of MiMa provides an aggressive reporting module for finding
all potential binary incompatibilties.

## Usage

The sbt plugin is released for sbt 1.x only (use 0.3.0 for sbt 0.13).  To try it, do the following:

Add the following to your `project/plugins.sbt` file:

```
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.4.0")
```

Add the following to your `build.sbt` file:

```
mimaPreviousArtifacts := Set("com.jsuereth" % "scala-arm_2.9.1" % "1.2") // replace with your old artifact id
```

Run `mimaReportBinaryIssues`.  You should see something like the following:

```
[info] Found 4 potential binary incompatibilities
[error]  * method rollbackTransactionResource()resource.Resource in object resource.Resource does not have a   correspondent in new version
[error]  * method now()scala.util.continuations.ControlContext in trait resource.ManagedResourceOperations does not    have a correspondent in old version
[error]  * abstract method now()scala.util.continuations.ControlContext in interface resource.ManagedResource does not have a correspondent in old version
[error]  * method rollbackTransactionResource()resource.Resource in trait resource.MediumPriorityResourceImplicits does not have a correspondent in new version
[error] {file:/home/jsuereth/project/personal/scala-arm/}scala-arm/*:mima-report-binary-issues: Binary compatibility check failed!
[error] Total time: 15 s, completed May 18, 2012 11:32:29 AM
```

## Advanced Usage (Filtering Binary Incompatibilities)

Sometimes you may want to filter out some binary incompatibility. For instance, because you warn your users to never use a class or method marked with a particular annotation (e.g., [`@experimental`](https://github.com/lightbend/mima/issues/160) - note, this annotation is **not** part of the Scala standard library), you may want to exclude all classes/methods that use such annotation. In MiMa you can do this by _explicitly list each binary incompatibility_ that you want to ignore (unfortunately, it doesn't yet support annotation-based filtering - PR is welcomed! :)). 

Open your `build.sbt` file, and

1. List the binary incompatibilities you would like to ignore

  ```
  val ignoredABIProblems = {
    import com.typesafe.tools.mima.core._
    import com.typesafe.tools.mima.core.ProblemFilters._
    Seq(
      exclude[MissingClassProblem]("akka.dispatch.SharingMailbox"),
      exclude[IncompatibleMethTypeProblem]("akka.dispatch.DefaultPromise.<<")
    )
  }
  ```

2. Configure MiMa to include the defined binary incompatibility ignores

  ```
  mimaPreviousArtifacts := Set("com.jsuereth" % "scala-arm_2.9.1" % "1.2") // replace with your old artifact id
  mimaBinaryIssueFilters ++= ignoredABIProblems
  ```

## FAQ

`java.lang.OutOfMemoryError - Java heap space:` If you are experiencing out of memory exception you may need to increase the VM arguments for the initial heap size and the maximum heap size. The default values are `-Xms64m` for for the initial heap size and `-Xmx256m` for the maximum heap size.
