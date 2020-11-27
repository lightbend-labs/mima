package foo

// All eight combinations of class/object nesting to three levels.
// O-O-O means object { object { object } }
// C-C-C means class  { class  { class  } }
// etc..
// Because of scala/bug#2034 we can't put these all in one package (you get "name clash" errors)
// So instead we'll split them in 4 nice and even packages
package l1  { object x { private[foo] def go11() = 11 }; class x { private[foo] def go12() = 12 }}
// l1/x.class:
// 0: MODULEsym x 8
// 1:  CLASSsym x 8
// 4:  CLASSsym x 8
//   MODULEsym + CLASSsym x <module> // x + x$
//      VALsym go11 private[foo]
//    CLASSsym x                     // x
//      VALsym go12 private[foo]

package l2a { object x { object y { private[foo] def go21() = 21 }; class y { private[foo] def go22() = 22 }}}
package l2b { class  x { object y { private[foo] def go23() = 23 }; class y { private[foo] def go24() = 24 }}}
// l2a/x.class:
// 0 MODULEsym x 11
// 1  CLASSsym x 11
// 3 MODULEsym y  1
// 4  CLASSsym y  1
// 7  CLASSsym y  1
//   MODULEsym + CLASSsym x <module>   // x + x$
//     MODULEsym + CLASSsym y <module> // x$y$
//      CLASSsym y                     // x$y

// l2b/x.class:
// 0  CLASSsym x 10
// 2 MODULEsym y  0
// 3  CLASSsym y  0
// 6  CLASSsym y  0
//   CLASSsym x                        // x
//     MODULEsym + CLASSsym y <module> // x$y$
//      CLASSsym y                     // x$y

package l3a { object x { object y { object z { private[foo] def go31() = 31 }; class z { private[foo] def go32() = 32 }}}}
package l3b { object x { class  y { object z { private[foo] def go33() = 33 }; class z { private[foo] def go34() = 34 }}}}
package l3c { class  x { object y { object z { private[foo] def go35() = 35 }; class z { private[foo] def go36() = 36 }}}}
package l3d { class  x { class  y { object z { private[foo] def go37() = 37 }; class z { private[foo] def go38() = 38 }}}}
// l3a/x.class:
//   0 MODULEsym x 14 + 1 CLASSsym x 14   // x + x$
//     3 MODULEsym y 1 + 4 CLASSsym y 1   // x$y$
//       6 MODULEsym z 4 + 7 CLASSsym z 4 // x$y$z$
//       10 CLASSsym z 4                  // x$y$z
//
// l3b/x.class:
//   0 MODULEsym x 13 + 1 CLASSsym x 13   // x + x$
//     3 CLASSsym y 1                     // x$y
//       5 MODULEsym z 3 + 6 CLASSsym z 3 // x$y$z$
//       9  CLASSsym z 3                  // x$y$z
//
// l3c/x.class:
//   0 CLASSsym x 13                      // x
//     2 MODULEsym y 0 + 3 CLASSsym y 0   // x$y$
//       5 MODULEsym z 3 + 6 CLASSsym z 3 // x$y$z$
//       9  CLASSsym z 3                  // x$y$z$
//
// l3d/x.class:
//   0 CLASSsym x 12                      // x
//     2 CLASSsym y 0                     // x$y
//       4 MODULEsym z 2 + 5 CLASSsym z 2 // x$y$z$
//       8  CLASSsym z 2                  // x$y$z

object Lib {
  def doIt = { doL1(); doL2(); doL3() }

  def doL1(): Unit = {
    val o =     l1.x
    val c = new l1.x()

    o.go11()
    c.go12()
  }

  def doL2(): Unit = {
    val o =     l2a.x
    val c = new l2b.x()

    val oo =     o.y
    val oc = new o.y()
    val co =     c.y
    val cc = new c.y()

    oo.go21()
    oc.go22()
    co.go23()
    cc.go24()
  }

  def doL3(): Unit = {
    val o1 =     l3a.x
    val o2 =     l3b.x
    val c3 = new l3c.x()
    val c4 = new l3d.x()

    val oo =     o1.y
    val oc = new o2.y()
    val co =     c3.y
    val cc = new c4.y()

    val ooo =     oo.z
    val ooc = new oo.z()
    val oco =     oc.z
    val occ = new oc.z()
    val coo =     co.z
    val coc = new co.z()
    val cco =     cc.z
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
