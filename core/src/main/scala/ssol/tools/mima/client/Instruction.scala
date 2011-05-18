package ssol.tools.mima.client

import scala.tools.nsc.symtab.classfile.{ClassfileConstants => JVM}

import ssol.tools.mima.core._

case class Instruction(offset: Int, instr: Int, argument: Int) {
  def hasReference: Boolean = instr match {
    case JVM.getstatic
      | JVM.putstatic
      | JVM.getfield 
      | JVM.putfield 
      | JVM.invokevirtual  
      | JVM.invokeinterface
      | JVM.invokespecial  
      | JVM.invokestatic => true
    case _ =>
      false
  }
  def target(cp: ClassfileParser): Reference = 
    if (hasReference) cp.pool.getReference(argument)
    else Reference(NoClass, "", "")
}

