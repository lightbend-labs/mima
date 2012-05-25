Migration Manager for Scala
==============

A tool for diagnosing and fixing migration problems for Scala libraries
=======================================================================

The Migration Manager for Scala (MiMa in short) is a tool for diagnosing binary incompatibilities for Scala libraries.

Please, use the [mima-user Mailing List][mima-user-ml] for questions and comments.

MiMa's Modules
-------

MiMa is split into Several modules: 

- Core: Classes that are used for both migrations and reporting.
- Core-UI: UI Classes that can be re-used between the migrator and the reporter.
- Reporter:  Raw reporting classes and the command line interface.
- Reporter-UI: Swing interface to the reporter.
- SBT Plugin:  The SBT plugin for usage inside SBT builds.


SBT Plugin
----------

The SBT Plugin is released for SBT version 0.11.3.  To try it, do the following:

1. Add the following to your `project/project/build.scala` file:

```
resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.3")
```

2. Add the following to your `build.sbt` file:

```
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
      
mimaDefaultSettings
      
previousArtifact := Some("com.jsuereth" % "scala-arm_2.9.1" % "1.2")
```


But replacing the scala-arm example with your own artifact.

3. Run `mima-report-binary-issues`.  You should see something like the following:

```
[info] Found 4 potential binary incompatibilities
[error]  * method rollbackTransactionResource()resource.Resource in object resource.Resource does not have a correspondent in new version
[error]  * method now()scala.util.continuations.ControlContext in trait resource.ManagedResourceOperations does not have a correspondent in old version
[error]  * abstract method now()scala.util.continuations.ControlContext in interface resource.ManagedResource does not have a correspondent in old version
[error]  * method rollbackTransactionResource()resource.Resource in trait resource.MediumPriorityResourceImplicits does not have a correspondent in new version
[error] {file:/home/jsuereth/project/personal/scala-arm/}scala-arm/*:mima-report-binary-issues: Binary compatibility check failed!
[error] Total time: 15 s, completed May 18, 2012 11:32:29 AM
```


Eclipse
-------

In order to create Eclipse metadata files (i.e., .classpath and .project) we recomend to use [sbteclipse][sbteclipse]. Once done, to set up the three modules in Eclipse just click on `File > Import > General > Exisiting Projects Into Workspace`, and select the MiMa project's root folder, the three modules should be correctly loaded.


[sbteclipse]: https://github.com/typesafehub/sbteclipse/

Build
-------

Using [the xsbt tool][xsbt]. 

      $ xsbt clean update compile


Make sure to use tag 0.11.3, installation notes can be found [here][xsbt].

[xsbt]: https://github.com/harrah/xsbt/tree/v0.11.3

This will recompile all MiMa's modules.

If you'd like to create distributable jar files run:

      $ xsbt assembly

This will create `reporter/target/mima-reporter-assembly-....jar` and `reporter-ui/target/mima-reporter-ui-assembly-....jar` jar files that can be used to launch the command line and ui version of MiMa.


Launch MiMa Reporter UI
-------
Type the following command to run the MiMa Reporter

	$ xsbt reporter-ui/run

Launch MiMa Reporter CLI
-------
Type the following command to run the MiMa Reporter command-line

	$ xsbt reporter/run

MiMa Reporter: Functional Tests
-------

The directory containing the MiMa Reporter module ('reporter') there is a 'functional-tests' folder that contains several functional tests exercising the system. All tests are executed as part of the build, therefore when running

	$ xsbt test-functional

if one (or more) test fails the build is stop and no jar will not be produced.

To add a new functional test to the suite, create a new folder within 'functional-tests' directory with the following structure:

	functional-tests
	    |
	    | --> <your-new-test-folder> (folder for your new test)
			|
			|-----> problems.txt (the expected list of reported errors - 1 line per error)
			|-----> v1 (folder containing sources @ version 1)
			|-----> v2 (folder containing sources @ version 2)

After doing that, `reload` if you are in a `xsbt` console session (if that makes nosense to you, it means you are fine and you can run the test as usual).

Tests within the `functional-tests` folder should always pass.

Note: The `problems.txt` is the test oracle. Expected errors are declared using the Mima's reporting output (i.e., the output of the tool and the expected errors should match perfectly). Admittedly, this coupling is an issue since the testing framework is highly coupled with the tool output used to report errors to the user. We should improve this and make the two independent. Until then, mind that by changing the output of the tool you will likely have to update some of the test oracles (i.e., problems.txt file).

FAQ
-------

`java.lang.OutOfMemoryError - Java heap space:` If you are experiencing out of memory exception you may need to increase the VM arguments for the initial heap size and the maximum heap size. The default values are `-Xms64m` for for the initial heap size and `-Xmx256m` for the maximum heap size.

Bugs and Feature requests
-------

Use the [GitHub project page][mima-github] for filing new tickets. For questions and comments, please use the [mima-user Mailing List][mima-user-ml].

[mima-github]: https://github.com/typesafehub/migration-manager/issues


Contributing
------------
If you'd like to contribute to the MiMa project, please sign the [contributor's licensing agreement](http://www.typesafe.com/contribute/cla).

License
-------
Copyright 2012 Typesafe, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[mima-user-ml]: https://groups.google.com/group/migration-manager-user/topics
