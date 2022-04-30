package foo

// All eight combinations of class/object nesting to three levels.
// O-O-O means object { object { object } }
// C-C-C means class  { class  { class  } }
// etc..
// Because of scala/bug#2034 we can't put these all in one package (you get "name clash" errors)
// So instead we'll split them in 4 nice and even packages
package l1 { object x { private[foo] def go11() = 11 }; class x { private[foo] def go12() = 12 } }
// l1/x.tasty:
// VALDEF x + TYPEDEF x[ModuleClass] // x + x$
// TYPEDEF x                         // x

package l2a { object x { object y { private[foo] def go21() = 21 }; class y { private[foo] def go22() = 22 } } }
package l2b { class x { object y { private[foo] def go23() = 23 }; class y { private[foo] def go24() = 24 } } }
// l2a/x.tasty:
// VALDEF x + TYPEDEF x[ModuleClass]   // x + x$
//   VALDEF y + TYPEDEF y[ModuleClass] // x$y$
//   TYPEDEF y                         // x$y

// l2b/x.tasty:
// TYPEDEF x                           // x
//   VALDEF y + TYPEDEF y[ModuleClass] // x$y$
//   TYPEDEF y                         // x$y

package l3a {
  object x { object y { object z { private[foo] def go31() = 31 }; class z { private[foo] def go32() = 32 } } }
}
package l3b {
  object x { class y { object z { private[foo] def go33() = 33 }; class z { private[foo] def go34() = 34 } } }
}
package l3c {
  class x { object y { object z { private[foo] def go35() = 35 }; class z { private[foo] def go36() = 36 } } }
}
package l3d {
  class x { class y { object z { private[foo] def go37() = 37 }; class z { private[foo] def go38() = 38 } } }
}
// l3a/x.tasty:
// VALDEF x + TYPEDEF x[ModuleClass]     // x + x$
//   VALDEF y + TYPEDEF y[ModuleClass]   // x$y$
//     VALDEF z + TYPEDEF z[ModuleClass] // x$y$z$
//       TYPEDEF z                       // x$y$z
//
// l3b/x.tasty:
// VALDEF x + TYPEDEF 5 x[ModuleClass]   // x + x$
//   TYPEDEF y                           // x$y
//     VALDEF z + TYPEDEF z[ModuleClass] // x$y$z$
//     TYPEDEF z                         // x$y$z
//
// l3c/x.tasty:
// TYPEDEF x                             // x
//   VALDEF y + TYPEDEF y[ModuleClass]   // x$y$
//     VALDEF z + TYPEDEF z[ModuleClass] // x$y$z$
//     TYPEDEF z                         // x$y$z
//
// l3d/x.tasty:
// TYPEDEF x                             // x
//   TYPEDEF y                           // x$y
//     VALDEF z + TYPEDEF z[ModuleClass] // x$y$z$
//     TYPEDEF z                         // x$y$z

object Lib {
  def doIt = { doL1(); doL2(); doL3() }

  def doL1(): Unit = {
    val o = l1.x
    val c = new l1.x()

    o.go11()
    c.go12()
  }

  def doL2(): Unit = {
    val o = l2a.x
    val c = new l2b.x()

    val oo = o.y
    val oc = new o.y()
    val co = c.y
    val cc = new c.y()

    oo.go21()
    oc.go22()
    co.go23()
    cc.go24()
  }

  def doL3(): Unit = {
    val o1 = l3a.x
    val o2 = l3b.x
    val c3 = new l3c.x()
    val c4 = new l3d.x()

    val oo = o1.y
    val oc = new o2.y()
    val co = c3.y
    val cc = new c4.y()

    val ooo = oo.z
    val ooc = new oo.z()
    val oco = oc.z
    val occ = new oc.z()
    val coo = co.z
    val coc = new co.z()
    val cco = cc.z
    val ccc = new cc.z()

    ooo.go31()
    ooc.go32()
    oco.go33()
    occ.go34()
    coo.go35()
    coc.go36()
    cco.go37()
    ccc.go38()
  }
}
