package com.typesafe.tools.mima.core

object MemberInfo {
  val ConstructorName = "<init>"
}

sealed abstract class MemberInfo(val owner: ClassInfo, val bytecodeName: String, val flags: Int, val descriptor: String)
    extends InfoLike
{
  final var isDeprecated: Boolean  = false
  final var signature: Signature   = Signature.none // Includes generics. 'descriptor' is the erased version.
  final var scopedPrivate: Boolean = false

  def nonAccessible: Boolean

  final def fullName: String          = s"${owner.formattedFullName}.$decodedName"
  final def abstractPrefix            = if (isDeferred && !owner.isTrait) "abstract " else ""
  final def staticPrefix: String      = if (isStatic) "static " else ""
  final def tpe: Type                 = owner.owner.definitions.fromDescriptor(descriptor)
  final def hasSyntheticName: Boolean = decodedName.contains('$')

  final def memberString: String = this match {
    case info: FieldInfo  => info.fieldString
    case info: MethodInfo => info.methodString
  }
}

private[mima] final class FieldInfo(owner: ClassInfo, bytecodeName: String, flags: Int, descriptor: String)
    extends MemberInfo(owner, bytecodeName, flags, descriptor)
{
  def nonAccessible: Boolean = !isPublic || isSynthetic || hasSyntheticName
  def fieldString: String    = s"${staticPrefix}field $decodedName in ${owner.classString}"
  override def toString      = s"field $bytecodeName: $descriptor"
}

private[mima] final class MethodInfo(owner: ClassInfo, bytecodeName: String, flags: Int, descriptor: String)
    extends MemberInfo(owner, bytecodeName, flags, descriptor)
{
  final var _annotations: List[AnnotInfo] = Nil
  final def annotations: List[AnnotInfo]  = _annotations

  def methodString: String      = s"$shortMethodString in ${owner.classString}"
  def shortMethodString: String = {
    val prefix = if (hasSyntheticName) if (isExtensionMethod) "extension " else "synthetic " else ""
    val deprecated = if (isDeprecated) "deprecated " else ""
    s"${abstractPrefix}$prefix${deprecated}${staticPrefix}method $decodedName$tpe"
  }

  lazy val paramsCount: Int = {
    tpe match {
      case MethodType(paramTypes, _) => paramTypes.length
      case _ => throw new MatchError(s"Failed to get method params, member had type $tpe, not MethodType.")
    }
  }

  assert(descriptor.charAt(0) == '(')
  def parametersDesc: String                  = descriptor.substring(1, descriptor.indexOf(")"))
  def matchesType(other: MethodInfo): Boolean = parametersDesc == other.parametersDesc

  private def isDefaultGetter: Boolean   = decodedName.contains("$default$")
  private def isTraitInit: Boolean       = decodedName == "$init$"
  private def isExtensionMethod: Boolean = {
    var i = decodedName.length - 1
    while (i >= 0 && Character.isDigit(decodedName.charAt(i)))
      i -= 1
    decodedName.substring(0, i + 1).endsWith("$extension")
  }
  def nonAccessible: Boolean = {
    !isPublic || isScopedPrivate || isSynthetic ||
      (hasSyntheticName && !(isExtensionMethod || isDefaultGetter || isTraitInit))
  }
  def isScopedPrivate: Boolean = scopedPrivate

  override def toString = s"def $bytecodeName: $descriptor"
}
