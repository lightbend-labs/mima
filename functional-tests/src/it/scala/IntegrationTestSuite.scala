package com.typesafe.tools.mima.lib

import munit._

class IntegrationTestSuite extends Suite {
  type TestValue = Unit

  def munitTests() = IntegrationTests.munitTests
}
