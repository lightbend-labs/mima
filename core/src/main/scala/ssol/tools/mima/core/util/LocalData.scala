package ssol.tools.mima.core.util

import scala.collection.mutable._

object LocalData {
  private val liceneseAgreed = "license-agreed"
  
  private val data = new HashMap[String,Class[_]] with SynchronizedMap[String,Class[_]]
  val licenseAgreed = {
    load(liceneseAgreed, classOf[Boolean])
  }
  
  private def load(key: String, tpe: Class[_]) {
    
  }
}