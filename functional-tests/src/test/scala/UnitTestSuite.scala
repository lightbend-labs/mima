package com.typesafe.tools.mima.lib

import munit._

class UnitTestSuite extends Suite {
  type TestValue = Unit

  def munitTests() = for {
    test <- TestCase.argsToTests(Nil, UnitTests.runTestCase).tests
  } yield new GenericTest(test.label, test.action, Set.empty, Location.empty)
}
