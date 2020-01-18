class A {
  def toHex(bytes: Array[Byte]): String = "A"
}

package p {
  class A {
    def toHex(bytes: Array[Byte]): String = "p.A"
  }
}

package p.q {
  class A {
    def toHex(bytes: Array[Byte]): String = "p.q.A"
  }
}

package p.q.r {
  class A {
    def toHex(bytes: Array[Byte]): String = "p.q.r.A"
  }
}

package p.q.r.s {
  class A {
    def toHex(bytes: Array[Byte]): String = "p.q.r.s.A"
  }
}
