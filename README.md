Migration Manager for Scala
==============

A tool for diagnosing and fixing migration problems for Scala libraries.
=======
The Migration Manager for Scala (MiMa in short) is a tool for diagnosing and fixing binary incompatibilities for Scala libraries.  The OSS version includes only
features which allow inspecting binary compatibility.

MiMa's Modules
-------

MiMa is split into Several modules: 

- Core: Classes that are used for both migrations and reporting.
- Core-UI: UI Classes that can be re-used between the migrator and the reporter.
- Reporter:  Raw reporting classes and the command line interface.
- Reporter-UI: Swing interface to the reporter.

Eclipse
-------

In order to create Eclipse metadata files (i.e., .classpath and .project) we recomend to use [sbteclipse][sbteclipse]. Once done, to set up the three modules in Eclipse just click on `File > Import > General > Exisiting Projects Into Workspace`, and select the MiMa project's root folder, the three modules should be correctly loaded.

In the `reporter-ui` folders you can also find a `*.launch` file configuration, which can be imported in Eclipse and can be used to start the programs.


[sbteclipse]: https://github.com/typesafehub/sbteclipse/

Build
-------

Using [the xsbt tool][xsbt]. 

      $ xsbt clean update compile


Make sure to use tag 0.11.2, installation notes can be found [here][xsbt].

[xsbt]: https://github.com/harrah/xsbt/tree/v0.11.2

This will recompile all MiMa's modules.


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

Use the [Assembla project page][mima-assembla] for filing new tickets.

[mima-assembla]: https://www.assembla.com/spaces/mima/tickets
