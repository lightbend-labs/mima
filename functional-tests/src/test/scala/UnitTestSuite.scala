package com.typesafe.tools.mima.lib

import munit._

class UnitTestSuite extends Suite {
  type TestValue = Unit

  def munitTests() = UnitTests.munitTests
}
