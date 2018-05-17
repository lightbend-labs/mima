Migration Manager for Scala
===========================

The Migration Manager for Scala (MiMa in short) is a tool for diagnosing binary incompatibilities for Scala libraries.

If you don't know how to use MiMa, please [read the user documentation](https://github.com/typesafehub/migration-manager/wiki).

MiMa's Modules
--------------

MiMa is split into Several modules:

- Core: classes that are used for detection.
- Reporter: reporting classes.
- SBT Plugin: the sbt plugin for usage inside sbt builds.

Usage
-----

To use MiMa as an sbt plugin, see the [sbt plugin wiki page](https://github.com/typesafehub/migration-manager/wiki/Sbt-plugin).


Build
-----

Using [sbt][sbt]:

      $ sbt compile

[sbt]: http://www.scala-sbt.org/

This will recompile all MiMa's modules.

If you'd like to create distributable jar files for the CLI, run:

      $ sbt assembly

This will create `reporter/target/mima-reporter-assembly-....jar` jar file that can be used to launch the command line version of MiMa.


MiMa Reporter: Functional Tests
-------------------------------

The directory containing the MiMa Reporter module ('reporter') there is a 'functional-tests' folder that contains several functional tests exercising the system. All tests are executed as part of the build, therefore when running

	$ sbt testFunctional

if one (or more) test fails the build is stop and no jar will not be produced.

To add a new functional test to the suite, create a new folder within 'functional-tests' directory with the following structure:

	functional-tests
	    |
	    | --> <your-new-test-folder> (folder for your new test)
			|
			|-----> problems.txt (the expected list of reported errors - 1 line per error)
			|-----> v1 (folder containing sources @ version 1)
			|-----> v2 (folder containing sources @ version 2)

After doing that, `reload` if you are in a `sbt` console session (if that makes no sense to you, it means you are fine and you can run the test as usual).

Tests within the `functional-tests` folder should always pass.

Note: The `problems.txt` is the test oracle. Expected errors are declared using the MiMa's reporting output (i.e., the output of the tool and the expected errors should match perfectly). Admittedly, this coupling is an issue since the testing framework is highly coupled with the tool output used to report errors to the user. We should improve this and make the two independent. Until then, mind that by changing the output of the tool you will likely have to update some of the test oracles (i.e., problems.txt file). When running tests against Scala 2.12 or higher, `problems-2.12.txt` is preferred over `problems.txt` if the former exists.

FAQ
---

`java.lang.OutOfMemoryError - Java heap space:` If you are experiencing out of memory exception you may need to increase the VM arguments for the initial heap size and the maximum heap size. The default values are `-Xms64m` for for the initial heap size and `-Xmx256m` for the maximum heap size.

Bugs and Feature requests
-------------------------

Use the [GitHub project page][mima-github] for filing new tickets.

[mima-github]: https://github.com/typesafehub/migration-manager/issues


Contributing
------------
If you'd like to contribute to the MiMa project, please sign the [contributor's licensing agreement](http://www.lightbend.com/contribute/cla).

License
-------
Copyright 2012-2016 Lightbend, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
