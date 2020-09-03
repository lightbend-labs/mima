package com.typesafe.tools.mima.lib

import javax.tools._

import scala.annotation.tailrec
import scala.reflect.io.Directory
import scala.util.Try

object TestCli {
  val scala211 = "2.11.12"
  val scala212 = "2.12.11"
  val scala213 = "2.13.2"
  val hostScalaVersion = scala.util.Properties.versionNumberString
  val allScalaVersions = List(scala211, scala212, scala213)
  val testsDir = Directory("functional-tests/src/test")

  def argsToTests(argv: List[String], runTestCase: TestCase => Try[Unit]): Tests =
    Tests(fromArgs(argv).map(testCaseToTest1(_, runTestCase)))

  def testCaseToTest1(tc: TestCase, runTestCase: TestCase => Try[Unit]): Test1 =
    Test(s"${tc.scalaBinaryVersion} / ${tc.name}", runTestCase(tc))

  def fromArgs(argv: List[String]): List[TestCase] = fromConf(go(argv, Conf(Nil, Nil)))

  @tailrec def postProcessConf(conf: Conf): Conf = conf match {
    case Conf(Nil, _) => postProcessConf(conf.copy(scalaVersions = List(hostScalaVersion)))
    case Conf(_, Nil) => postProcessConf(conf.copy(dirs = allTestDirs()))
    case _            => conf
  }

  def fromConf(conf: Conf): List[TestCase] = {
    val conf1 = postProcessConf(conf)
    val javaCompiler = ToolProvider.getSystemJavaCompiler
    for {
      sc <- conf1.scalaVersions.map(new ScalaCompiler(_))
      dir <- conf1.dirs
    } yield new TestCase(dir, sc, javaCompiler)
  }

  def allTestDirs() = testsDir.dirs.filter(_.files.exists(_.name == "problems.txt")).toList.sortBy(_.path)

  final case class Conf(scalaVersions: List[String], dirs: List[Directory])

  @tailrec private def go(argv: List[String], conf: Conf): Conf = argv match {
    case "-213" :: xs                  => go(xs, conf.copy(scalaVersions = scala213 :: conf.scalaVersions))
    case "-212" :: xs                  => go(xs, conf.copy(scalaVersions = scala212 :: conf.scalaVersions))
    case "-211" :: xs                  => go(xs, conf.copy(scalaVersions = scala211 :: conf.scalaVersions))
    case "--scala-version" :: sv :: xs => go(xs, conf.copy(scalaVersions = sv :: conf.scalaVersions))
    case "--cross" :: xs               => go(xs, conf.copy(scalaVersions = List(scala211, scala212, scala213)))
    case s :: xs                       => go(xs, conf.copy(dirs = testDirs(s) ::: conf.dirs))
    case Nil                           => conf
  }

  /** All the directories in the tests directory whose name contain the given string `s`. */
  def testDirs(s: String): List[Directory] = {
    testsDir.dirs.filter(_.name.contains(s)).toList.sortBy(_.path) match {
      case Nil  => sys.error(s"No such directory: ${testsDir / s}")
      case dirs => dirs
    }
  }

  if (System.out != Console.out) {
    System.out.println(" System.out identity ##: " + System.identityHashCode(System.out))
    System.out.println("Console.out identity ##: " + System.identityHashCode(scala.Console.out))
    System.out.println("cwd: " + new java.io.File("").getAbsoluteFile)
  }
}
