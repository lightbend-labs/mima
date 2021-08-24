package com.typesafe.tools.mima.lib

import scala.reflect.io.Directory

import IntegrationTests._

class IntegrationTestSuite extends munit.FunSuite {
  itTest("scala-library-2-10")
  itTest("scala-library-2-11")
  itTest("scala-library-2-12")
  itTest("scala-library-2-13")

  itTest("scala-reflect-2-10")
  itTest("scala-reflect-2-11")
  itTest("scala-reflect-2-12")
  itTest("scala-reflect-2-13")

  itTest("java-9-module-info")

  test("scala3-library")(testIntegration("org.scala-lang", "scala3-library_3", "3.0.0", "3.0.1")(
    excludeAnnots = List("scala.annotation.experimental"),
  ))

  def itTest(name: String)(implicit loc: munit.Location) = test(name) {
    testIntegrationDir(Directory(s"functional-tests/src/it/$name")).get
  }
}
