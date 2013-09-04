Migration Manager for Scala
==============

A tool for diagnosing migration problems for Scala libraries
============================================================

The Migration Manager for Scala (MiMa in short) is a tool for diagnosing binary incompatibilities for Scala libraries.

If you don't know how to use MiMa, please [read the user documentation](https://github.com/typesafehub/migration-manager/wiki).

Please, use the [mima-user Mailing List][mima-user-ml] for questions and comments.

MiMa's Modules
-------

MiMa is split into Several modules:

- Core: Classes that are used for detection.
- Core-UI: UI Classes that can be re-used between different detectors.
- Reporter:  Raw reporting classes and the command line interface.
- Reporter-UI: Swing interface to the reporter.
- SBT Plugin:  The SBT plugin for usage inside SBT builds.

Eclipse
-------

In order to create Eclipse metadata files (i.e., .classpath and .project) we recomend to use [sbteclipse][sbteclipse].

Setting up [sbteclipse][sbteclipse] is a simple three-steps process:

* Create a ``eclipse.sbt`` file under the ``project`` folder and add the [sbteclipse][sbteclipse] plugin.
At the time of this writing, my ``project/eclipse.sbt`` contains the following:

	``addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.3.0")``

* Start ``sbt`` from command line on the project's root, and execute the following command ``eclipse`` (or ``eclipse with-source=true`` if you want source attachments correctly set)

* Once done, import all modules in Eclipse, i.e., just click on `File > Import > General > Exisiting Projects Into Workspace`, select the MiMa project's root folder, all modules should be correctly loaded.


[sbteclipse]: https://github.com/typesafehub/sbteclipse/

Build
-------

Using [sbt][sbt] 0.13.0 or newer.

      $ sbt compile

[sbt]: http://www.scala-sbt.org/

This will recompile all MiMa's modules.

If you'd like to create distributable jar files run:

      $ sbt assembly

This will create `reporter/target/mima-reporter-assembly-....jar` and `reporter-ui/target/mima-reporter-ui-assembly-....jar` jar files that can be used to launch the command line and ui version of MiMa.


Launch MiMa Reporter UI
-------
Type the following command to run the MiMa Reporter

	$ sbt reporter-ui/run

Launch MiMa Reporter CLI
-------
Type the following command to run the MiMa Reporter command-line

	$ sbt reporter/run

MiMa Reporter: Functional Tests
-------

The directory containing the MiMa Reporter module ('reporter') there is a 'functional-tests' folder that contains several functional tests exercising the system. All tests are executed as part of the build, therefore when running

	$ sbt test-functional

if one (or more) test fails the build is stop and no jar will not be produced.

To add a new functional test to the suite, create a new folder within 'functional-tests' directory with the following structure:

	functional-tests
	    |
	    | --> <your-new-test-folder> (folder for your new test)
			|
			|-----> problems.txt (the expected list of reported errors - 1 line per error)
			|-----> v1 (folder containing sources @ version 1)
			|-----> v2 (folder containing sources @ version 2)

After doing that, `reload` if you are in a `sbt` console session (if that makes nosense to you, it means you are fine and you can run the test as usual).

Tests within the `functional-tests` folder should always pass.

Note: The `problems.txt` is the test oracle. Expected errors are declared using the Mima's reporting output (i.e., the output of the tool and the expected errors should match perfectly). Admittedly, this coupling is an issue since the testing framework is highly coupled with the tool output used to report errors to the user. We should improve this and make the two independent. Until then, mind that by changing the output of the tool you will likely have to update some of the test oracles (i.e., problems.txt file).

Releasing
---------
MiMa releases to several different locations:

1. Maven central (libraries)
2. repo.scala-sbt.org  (sbt plugin)
3. downloads.typesafe.com  (bundled CLI + UI artifacts).

To deploy to locations 1 and 2, simple use the `publish` task, but make sure your credentials for both Sonatype's OSSRH + repo.scala-sbt.org are in order.   After deploying, you need to log in to [OSSRH](http://oss.sonatype.org) and close/release the repository.

To deploy to downloads.typesafe.com, you need to run the `s3-upload` task, ensuring the appropriate credentials are in place (See [sbt-s3](https://github.com/sbt/sbt-s3) for more info).

MiMa releases should also update the content found on the [Typesafe Website](http://typesafe.com/technology/migration-manager).


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
