package com.typesafe.tools.mima.cli

import java.io.File
import java.nio.file.Files

final class MainSpec extends munit.FunSuite {
  val existingDir = new File(".") // exists but contributes no classes (like a POM-only module)
  val missing     = new File("does-not-exist.jar")

  test("a non-existent oldfile is an error") {
    val ex = intercept[IllegalArgumentException] {
      Main(oldBinOpt = Some(missing), newBinOpt = Some(existingDir)).run()
    }
    assert(ex.getMessage.contains("oldfile does not exist"), ex.getMessage)
  }

  test("a non-existent newfile is an error") {
    val ex = intercept[IllegalArgumentException] {
      Main(oldBinOpt = Some(existingDir), newBinOpt = Some(missing)).run()
    }
    assert(ex.getMessage.contains("newfile does not exist"), ex.getMessage)
  }

  test("existing but class-less paths are not an error (POM-only-style)") {
    val empty = Files.createTempDirectory("mima-cli-test").toFile
    try {
      val problems = Main(oldBinOpt = Some(empty), newBinOpt = Some(empty)).run()
      assertEquals(problems, 0)
    } finally empty.delete()
  }
}
