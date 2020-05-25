package com.typesafe.tools.mima.core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class SignatureSpec extends AnyWordSpec with Matchers {
  val promiseSig =
    "Lscala/concurrent/Promise<" +
      "Lscala/Function1<" +
        "Lscala/concurrent/duration/FiniteDuration;" +
        "Lscala/concurrent/Future<Lakka/http/scaladsl/Http$HttpTerminated;>;" +
      ">;" +
    ">;"

  val `signature_in_2.12.8` = Signature(s"(Lakka/http/impl/engine/server/GracefulTerminatorStage;$promiseSig)V")
  val `signature_in_2.12.9` = Signature(s"($promiseSig)V")

  "The method checker" should {
    "allow dropping the first parameter of the Signature attribute of a constructor" in {
      // Assuming the descriptor is the same,
      // dropping the first parameter of the Signature attribute
      // can only be explained by going from a Scala version that does not have
      // the fix in scala#7975 (2.12.8, 2.13.0) to one that does
      assert(`signature_in_2.12.8`.matches(`signature_in_2.12.9`, true))
    }

    "reject adding the first parameter of the Signature attribute of a constructor back" in {
      assert(!`signature_in_2.12.9`.matches(`signature_in_2.12.8`, true))
    }

    "allow renaming a generic parameter" in {
      val withU = Signature("<U:Ljava/lang/Object;>(TU;Lscala/collection/immutable/List<TU;>;)Lscala/Option<TU;>;")
      val withT = Signature("<T:Ljava/lang/Object;>(TT;Lscala/collection/immutable/List<TT;>;)Lscala/Option<TT;>;")

      assert(withU.matches(withT, false))
    }
  }

  "The signature parser" should {
    "parse a signature with generic bounds that themselves have generics" in {
      val (types, rest) = Signature.FormalTypeParameter.parseList(
        "T:Ljava/lang/Object;U:Lscala/collection/immutable/List<TT;>;>(TT;Lscala/collection/immutable/List<TU;>;)Lscala/Option<TT;>;>")
      types.length should be(2)
      types(0).identifier should be("T")
      types(0).bound should be("Ljava/lang/Object")
      types(1).identifier should be("U")
      types(1).bound should be("Lscala/collection/immutable/List<TT;>")
      rest should be("(TT;Lscala/collection/immutable/List<TU;>;)Lscala/Option<TT;>;>")
    }
  }
}
