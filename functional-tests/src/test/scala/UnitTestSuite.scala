package com.typesafe.tools.mima.lib

import munit._

class UnitTestSuite extends Suite {
  type TestValue = Unit

  def munitTests() = for {
    test <- TestCli.argsToTests(argv = Nil, UnitTests.runTestCase).tests
  } yield new GenericTest(test.label, test.action, Set.empty, Location.empty)
}
