package ssol.tools.mima

import scala.reflect.NameTransformer

object MemberInfo {

 /** The index of the string $_setter_$ in this string */
  private def setterIdx(name: String) = name.indexOf(setterTag)
  
  private val setterTag = "$_setter_$"
  private val setterSuffix = "_$eq"

  def maybeSetter(name: String) = name.endsWith(setterSuffix)
}

class MemberInfo(val owner: ClassInfo, val name: String, override val flags: Int, val sig: String) extends WithAccessFlags {
  override def toString = "def "+name+": "+ sig

  def decodedName = NameTransformer.decode(name)

  def fieldString = "field "+decodedName+" in "+owner.classString
  def shortMethodString = (if(hasSyntheticName) "synthetic " else "") + (if(isDeprecated) "deprecated " else "") + "method "+decodedName + tpe
  def methodString = shortMethodString + " in " + owner.classString
  def defString = (if(isDeprecated) "@deprecated " else "") + "def " + decodedName + params.mkString("(", ",", ")") + ": " + tpe.resultType + " = " 
  def applyString = decodedName + params.mkString("(", ",", ")")
  
  lazy val params: List[String] = tpe match {
    case MethodType(paramTypes, resultType) =>
      for ((ptype, index) <- paramTypes.zipWithIndex) yield "par" + index + ": " + ptype
  }
  
  def fullName = owner.formattedFullName + "." + decodedName

  def tpe: Type = owner.owner.definitions.fromSig(sig)

  def staticImpl = owner.implClass.staticImpl(this)

  def isMethod: Boolean = sig(0) == '('

  def parametersSig = {
    assert(isMethod)
    sig substring (1, sig indexOf ")")
  }

  def matchesType(other: MemberInfo): Boolean = 
    if (isMethod) other.isMethod && parametersSig == other.parametersSig
    else !other.isMethod && sig == other.sig

  def resultSig = {
    assert(sig(0) == '(')
    sig substring ((sig indexOf ")") + 1)
  }
  
  
  var codeOpt: Option[(Int, Int)] = None

  def isClassConstructor = name == "<init>"

  def needCode = isClassConstructor

  import MemberInfo._

  var isTraitSetter = maybeSetter(name) && setterIdx(name) >= 0

  var isDeprecated = false	

  def hasSyntheticName: Boolean = decodedName contains '$'
  
  def isAccessible: Boolean = isPublic && !hasSyntheticName

  /** The name of the getter corresponding to this setter */
  private def getterName: String = {
    val sidx = setterIdx(name)
    val start = if (sidx >= 0) sidx + setterTag.length else 0
    name.substring(start, name.length - setterSuffix.length)
  }

  /** The getter that corresponds to this setter */
  def getter: MemberInfo = {
    val argsig = "()" + parametersSig
    owner.methods.get(getterName) find (_.sig == argsig) get
  }

  def description: String = name+": "+sig+" from "+owner.description
}

