package ssol.tools.mima.core.util

import org.specs2.mutable._
import org.specs2.mock._
import org.specs2.specification._

class BrowseSpec extends SpecificationWithJUnit with Mockito {

  /** This is needed only to interoperate with Mockito as methods
   *  must explicitly declare the list of checked exception that may
   *  throw if you want to stub them.
   */
  trait BrowserProxyStub extends BrowserProxy {
    import java.net.URI
    @throws(classOf[java.io.IOException])
    @throws(classOf[java.lang.SecurityException])
    def open(webpage: URI)
  }

  private val anyOkUrl = "http://www.typesafe.com"
  private val anyNotOkUrl = "www.typesafe.com/migration manager.html" // space not allowed!

  private def anyURI = any[java.net.URI]

  trait SetUp extends Scope {
    val proxy = mock[BrowserProxyStub]
    val browse = new Browse(proxy)
  }

  "Browse" should {
    "succeed for a well-formed plain-text http url" in new SetUp {
      browse to anyOkUrl

      there was 1.times(proxy).open(any)
    }

    "fail for a malformed plain-text url" in new SetUp {
      browse to anyNotOkUrl

      there was one(proxy).cannotOpen(anyString, any)
    }

    "fail gracefully if the browser fails to be launched" in new SetUp {
      proxy.open(any) throws new java.io.IOException()

      browse to anyOkUrl

      there was one(proxy).cannotOpen(anyURI, any)
    }

    "fail gracefully if user has not enough privileges to launch the browser" in new SetUp {
      proxy.open(any) throws new java.lang.SecurityException()

      browse to anyOkUrl

      there was one(proxy).cannotOpen(anyURI, any)
    }

    "fail gracefully if the OS does not support Desktop.Action.OPEN" in new SetUp {
      proxy.open(any) throws new java.lang.UnsupportedOperationException()

      browse to anyOkUrl

      there was one(proxy).cannotOpen(anyURI, any)
    }
  }
}