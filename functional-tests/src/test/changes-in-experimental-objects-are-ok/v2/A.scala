package mima
package pkg2

import mima.annotation.exclude

@exclude object O {
  class  OC { class OCC; object OCO }
  object OO { class OOC; object OOO }
}
@exclude class C {
  class  CC { class CCC; object CCO }
  object CO { class COC; object COO }
}

object PrefixO {
  @exclude object O {
    class  OC { class OCC; object OCO }
    object OO { class OOC; object OOO }
  }
  @exclude class C {
    class  CC { class CCC; object CCO }
    object CO { class COC; object COO }
  }
}
class PrefixC {
  @exclude object O {
    class  OC { class OCC; object OCO }
    object OO { class OOC; object OOO }
  }
  @exclude class C {
    class  CC { class CCC; object CCO }
    object CO { class COC; object COO }
  }
}
