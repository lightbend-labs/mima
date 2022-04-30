package com.typesafe.tools.mima.core

final class SignatureSpec extends munit.FunSuite {
  val promiseSig =
    "Lscala/concurrent/Promise<" +
      "Lscala/Function1<" +
      "Lscala/concurrent/duration/FiniteDuration;" +
      "Lscala/concurrent/Future<Lakka/http/scaladsl/Http$HttpTerminated;>;" +
      ">;" +
      ">;"

  val `signature_in_2.12.8` = Signature(s"(Lakka/http/impl/engine/server/GracefulTerminatorStage;$promiseSig)V")
  val `signature_in_2.12.9` = Signature(s"($promiseSig)V")

  test("The method checker should allow dropping the first parameter of the Signature attribute of a constructor") {
    // Assuming the descriptor is the same,
    // dropping the first parameter of the Signature attribute
    // can only be explained by going from a Scala version that does not have
    // the fix in scala#7975 (2.12.8, 2.13.0) to one that does
    assert(`signature_in_2.12.8`.matches(`signature_in_2.12.9`, true))
  }

  test("The method checker should reject adding the first parameter of the Signature attribute of a constructor back") {
    assert(!`signature_in_2.12.9`.matches(`signature_in_2.12.8`, true))
  }

  test("The method checker should allow renaming a generic parameter") {
    val withU = Signature("<U:Ljava/lang/Object;>(TU;Lscala/collection/immutable/List<TU;>;)Lscala/Option<TU;>;")
    val withT = Signature("<T:Ljava/lang/Object;>(TT;Lscala/collection/immutable/List<TT;>;)Lscala/Option<TT;>;")

    assert(withU.matches(withT, false))
  }

  test("The signature parser should parse a signature with generic bounds that themselves have generics") {
    import Signature.FormalTypeParameter
    val rest             = "(TT;Lscala/collection/immutable/List<TU;>;)Lscala/Option<TT;>;>"
    val orig             = s"T:Ljava/lang/Object;U:Lscala/collection/immutable/List<TT;>;>$rest"
    val (types, obtRest) = FormalTypeParameter.parseList(orig)
    assertEquals(types.length, 2)
    assertEquals(types(0), FormalTypeParameter("T", "Ljava/lang/Object"))
    assertEquals(types(1), FormalTypeParameter("U", "Lscala/collection/immutable/List<TT;>"))
    assertEquals(obtRest, rest)
  }
}
