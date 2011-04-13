Migration Manager for Scala
==============

A tool for diagnosing and fixing migration problems for Scala libraries.

Build
-------

Using [the xsbt tool][xsbt]. 

      $ xsbt update compile


Make sure to build the sources of branch 0.9, installation notes can be found in the README.md file of the [xsbt git repository][xsbt].

[xsbt]: https://github.com/harrah/xsbt/tree/0.9

Functional Tests
-------

The 'functional-tests' folder contains a good number of functional tests that exercise the Mima's error reporting functionality. All tests are executed by running the following command:

	$ xsbt package

If you desire prefer to run a single test, then run the following command

	$ xsbt <test-folder-name>/fun-tests

To create add a new functional test to the suite, create a new folder within 'functional-tests' directory with the following structure:

	functional-tests
	    |
	    | --> <your-new-test> (folder for your new test)
			|
			|-----> problems.txt (the expected list of reported errors - 1 line per error)
			|-----> v1 (folder containing sources @ version 1)
			|-----> v2 (folder containing sources @ version 2)

After doing that, `reload` if you are in a `xsbt` console session (don't worry if that makes nosense to you, then you are fine and you can run the test as usual).

All tests in this folder should always pass. If you spot a bug/missing feature then you can create a test that demonstrates the problem and put it in the `functional-tests-exposing-bugs` directory, located in the project root.

Bugs and Feature requests
-------

Use the [Assembla project page][mima-assembla] for filing new tickets.

[mima-assembla]: https://www.assembla.com/spaces/mima/tickets