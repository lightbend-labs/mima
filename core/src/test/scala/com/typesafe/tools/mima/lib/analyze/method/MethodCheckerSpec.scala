package com.typesafe.tools.mima.lib.analyze.method

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class MethodCheckerSpec extends AnyWordSpec with Matchers {
  val promiseSig =
    "Lscala/concurrent/Promise<" +
      "Lscala/Function1<" +
        "Lscala/concurrent/duration/FiniteDuration;" +
        "Lscala/concurrent/Future<Lakka/http/scaladsl/Http$HttpTerminated;>;" +
      ">;" +
    ">;"

  val `signature_in_2.12.8` = s"(Lakka/http/impl/engine/server/GracefulTerminatorStage;$promiseSig)V"
  val `signature_in_2.12.9` = s"($promiseSig)V"

  "The method checker" should {
    "allow dropping the first parameter of the Signature attribute of a constructor" in {
      // Assuming the descriptor is the same,
      // dropping the first parameter of the Signature attribute
      // can only be explained by going from a Scala version that does not have
      // the fix in scala#7975 (2.12.8, 2.13.0) to one that does
      assert(MethodChecker.hasMatchingCtorSig(`signature_in_2.12.8`, `signature_in_2.12.9`))
    }

    "reject adding the first parameter of the Signature attribute of a constructor back" in {
      assert(!MethodChecker.hasMatchingCtorSig(`signature_in_2.12.9`, `signature_in_2.12.8`))
    }
  }
}
