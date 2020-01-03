class A {
  def toHex(bytes: Array[Byte], b: Boolean): String = "A"
}

package p {
  class A {
    def toHex(bytes: Array[Byte], b: Boolean): String = "p.A"
  }
}

package p.q {
  class A {
    def toHex(bytes: Array[Byte], b: Boolean): String = "p.q.A"
  }
}

package p.q.r {
  class A {
    def toHex(bytes: Array[Byte], b: Boolean): String = "p.q.r.A"
  }
}

package p.q.r.s {
  class A {
    def toHex(bytes: Array[Byte], b: Boolean): String = "p.q.r.s.A"
  }
}
