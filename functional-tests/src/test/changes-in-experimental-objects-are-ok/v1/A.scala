package mima
package pkg2

import mima.annotation.exclude

@exclude object O {
  def foo = 1
  class  OC { def foo = 1; class OCC { def foo = 1 }; object OCO { def foo = 1 } }
  object OO { def foo = 1; class OOC { def foo = 1 }; object OOO { def foo = 1 } }
}
@exclude class C {
  def foo = 1
  class  CC { def foo = 1; class CCC { def foo = 1 }; object CCO { def foo = 1 } }
  object CO { def foo = 1; class COC { def foo = 1 }; object COO { def foo = 1 } }
}

object PrefixO {
  @exclude object O {
    def foo = 1
    class  OC { def foo = 1; class OCC { def foo = 1 }; object OCO { def foo = 1 } }
    object OO { def foo = 1; class OOC { def foo = 1 }; object OOO { def foo = 1 } }
  }
  @exclude class C {
    def foo = 1
    class  CC { def foo = 1; class CCC { def foo = 1 }; object CCO { def foo = 1 } }
    object CO { def foo = 1; class COC { def foo = 1 }; object COO { def foo = 1 } }
  }
}
class PrefixC {
  @exclude object O {
    def foo = 1
    class  OC { def foo = 1; class OCC { def foo = 1 }; object OCO { def foo = 1 } }
    object OO { def foo = 1; class OOC { def foo = 1 }; object OOO { def foo = 1 } }
  }
  @exclude class C {
    def foo = 1
    class  CC { def foo = 1; class CCC { def foo = 1 }; object CCO { def foo = 1 } }
    object CO { def foo = 1; class COC { def foo = 1 }; object COO { def foo = 1 } }
  }
}
