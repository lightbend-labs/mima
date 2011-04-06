package ssol.tools.mima

import scala.tools.nsc.symtab.classfile.{ClassfileConstants => JVM}

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

