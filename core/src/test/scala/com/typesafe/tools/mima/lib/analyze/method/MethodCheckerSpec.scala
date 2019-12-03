package com.typesafe.tools.mima.lib.analyze.method

import com.typesafe.tools.mima.core.MemberInfo

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class MethodCheckerSpec extends AnyWordSpec with Matchers {
    "The method checker" should {
        val `signatureOn2.12.8` =
            "(Lakka/http/impl/engine/server/GracefulTerminatorStage;Lscala/concurrent/Promise<Lscala/Function1<Lscala/concurrent/duration/FiniteDuration;Lscala/concurrent/Future<Lakka/http/scaladsl/Http$HttpTerminated;>;>;>;)V"
        val `signatureOn2.12.9` =
            "(Lscala/concurrent/Promise<Lscala/Function1<Lscala/concurrent/duration/FiniteDuration;Lscala/concurrent/Future<Lakka/http/scaladsl/Http$HttpTerminated;>;>;>;)V"
        
        "allow dropping the first parameter of the Signature attribute of a constructor" in {
            // Assuming the descriptor is the same, dropping the first
            // parameter of the Signature attribute can only be explained by
            // going from a Scala version that does not have the fix in
            // https://github.com/scala/scala/pull/7975 (2.12.8, 2.13.0) to
            // one that does
            MethodChecker.hasMatchingSignature(
                `signatureOn2.12.8`,
                `signatureOn2.12.9`,
                MemberInfo.ConstructorName
            ) should be(true)
        }

        "reject adding the first parameter of the Signature attribute of a constructor back" in {
            MethodChecker.hasMatchingSignature(
                `signatureOn2.12.9`,
                `signatureOn2.12.8`,
                MemberInfo.ConstructorName
            ) should be(false)
        }
    }
}
