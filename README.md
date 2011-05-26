Migration Manager for Scala
==============

A tool for diagnosing and fixing migration problems for Scala libraries.

Build
-------

Using [the xsbt tool][xsbt]. 

      $ xsbt update compile


Make sure to build the sources of branch 0.9, installation notes can be found [here][xsbt].

[xsbt]: https://github.com/harrah/xsbt/tree/0.9


Launch MiMa
-------

	$ xsbt run


Functional Tests
-------

The 'functional-tests' folder contains a number of functional tests that exercise the Mima's error reporting functionality. All tests are executed by running the following command:

	$ xsbt package

If you prefer to run a single test, use the following command

	$ xsbt <test-folder-name>/fun-tests

To add a new functional test to the suite, create a new folder within 'functional-tests' directory with the following structure:

	functional-tests
	    |
	    | --> <your-new-test-folder> (folder for your new test)
			|
			|-----> problems.txt (the expected list of reported errors - 1 line per error)
			|-----> v1 (folder containing sources @ version 1)
			|-----> v2 (folder containing sources @ version 2)

After doing that, `reload` if you are in a `xsbt` console session (if that makes nosense to you, it means you are fine and you can run the test as usual).

Tests within the `functional-tests` folder should always pass. If you spot a bug (or missing feature), please create a test that exercise the problem and put it in the `functional-tests-exposing-bugs` directory, located in the project root. Once the test succeed, then you should move it into the `functional-tests` directory.

Note: The `problems.txt` is the test oracle. Expected errors are declared using the Mima's reporting output (i.e., the output of the tool and the expected errors should match perfectly). Admittedly, this coupling is an issue since the testing framework is highly coupled with the tool output used to report errors to the user. We should improve this and make the two independent. Until then, mind that by changing the output of the tool you will likely have to update some of the test oracles (i.e., problems.txt file).

FAQ
-------

Exception - java.lang.OutOfMemoryError: Java heap space: If you are experiencing out of memory exception you may need to increase the VM arguments for the initial heap size and the maximum heap size. The default values are `-Xms64m` for for the initial heap size and `-Xmx256m` for the maximum heap size.

Bugs and Feature requests
-------

Use the [Assembla project page][mima-assembla] for filing new tickets.

[mima-assembla]: https://www.assembla.com/spaces/mima/tickets
