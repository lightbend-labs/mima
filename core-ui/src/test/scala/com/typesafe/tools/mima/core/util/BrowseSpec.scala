package com.typesafe.tools.mima.core.util

import org.specs2.mutable._
import org.specs2.mock._
import org.specs2.specification._

class BrowseSpec extends SpecificationWithJUnit with Mockito {

  /** This is needed only to interoperate with `Mockito` as methods
   *  must explicitly declare all checked exception that may be thrown
   *  if you want to stub them to test graceful recovery from failure.
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

  trait BrowseMock extends Scope {
    val proxy = mock[BrowserProxyStub]
    val browse = new Browse(proxy)
  }

  "Browse" should {
    "succeed for a well-formed plain-text http url" in new BrowseMock {
      browse to anyOkUrl

      there was 1.times(proxy).open(any)
    }

    "fail for a malformed plain-text url" in new BrowseMock {
      browse to anyNotOkUrl

      there was one(proxy).cannotOpen(anyString, any)
    }

    "fail gracefully if the browser fails to be launched" in new BrowseMock {
      proxy.open(any) throws new java.io.IOException()

      browse to anyOkUrl

      there was one(proxy).cannotOpen(anyURI, any)
    }

    "fail gracefully if the user has not enough privileges to use the browser" in new BrowseMock {
      proxy.open(any) throws new java.lang.SecurityException()

      browse to anyOkUrl

      there was one(proxy).cannotOpen(anyURI, any)
    }

    "fail gracefully if the OS does not support Desktop.Action.BROWSE" in new BrowseMock {
      proxy.open(any) throws new java.lang.UnsupportedOperationException()

      browse to anyOkUrl

      there was one(proxy).cannotOpen(anyURI, any)
    }
    
    "fail gracefully if the URI cannot be converted into an URL" in new BrowseMock {
      proxy.open(any) throws new java.lang.IllegalArgumentException()

      browse to anyOkUrl

      there was one(proxy).cannotOpen(anyURI, any)
    }
  }
}