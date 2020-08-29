package com.typesafe.tools.mima.lib

import munit._

class IntegrationTestSuite extends Suite {
  type TestValue = Unit

  def munitTests() = for {
    test <- IntegrationTests.fromArgs(Nil).tests
  } yield new GenericTest(test.label, test.action, Set.empty, Location.empty)
}
