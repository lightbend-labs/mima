package com.typesafe.tools.mima.core

import scala.annotation.tailrec
import scala.tools.nsc.symtab.classfile.ClassfileConstants._
import scala.tools.nsc.util.ClassPath

/** This class holds together a root package and a classpath.
 *  It also offers definitions of commonly used classes, including java.lang.Object.
 *
 *  Each version of the input jar file has an instance of Definitions,
 *  used to resolve type names during classfile parsing.
 */
final class Definitions(val classPath: ClassPath) {
  lazy val root: PackageInfo          = new DefinitionsPackageInfo(this)
  lazy val ObjectClass: ClassInfo     = fromName("java.lang.Object")
  lazy val AnnotationClass: ClassInfo = fromName("java.lang.annotation.Annotation")

  /** Return the class corresponding to the fully qualified name.
   *  If there is no such class in the current classpath, a SyntheticClassInfo
   *  and all necessary SyntheticPackageInfo are created along the way.
   */
  def fromName(name: String): ClassInfo = {
    @tailrec def loop(pkg: PackageInfo, parts: List[String]): ClassInfo = parts match {
      case Nil      => NoClass
      case c :: Nil => pkg.classes.getOrElse(c, new SyntheticClassInfo(pkg, c))
      case p :: rem => loop(pkg.packages.getOrElseUpdate(p, new SyntheticPackageInfo(pkg, p)), rem)
    }
    loop(root, name.split("\\.").toList)
  }

  /** Return the type corresponding to this descriptor.
   *  Class names are resolved relative to the current classpath.
   */
  def fromDescriptor(descriptor: String): Type = {
    var in = 0

    def getType(): Type = {
      val ch = descriptor(in)
      in += 1
      ch match {
        case VOID_TAG   => Type.unitType
        case BOOL_TAG   => Type.booleanType
        case BYTE_TAG   => Type.byteType
        case SHORT_TAG  => Type.shortType
        case CHAR_TAG   => Type.charType
        case INT_TAG    => Type.intType
        case LONG_TAG   => Type.longType
        case FLOAT_TAG  => Type.floatType
        case DOUBLE_TAG => Type.doubleType
        case OBJECT_TAG => newClassType
        case ARRAY_TAG  => ArrayType(getType())
        case '('        => newMethodType
        case _          => throw new MatchError(s"unknown signature: ${descriptor.substring(in)}")
      }
    }

    def newClassType: ClassType = {
      val end = descriptor.indexOf(';', in)
      val fullName = descriptor.substring(in, end).replace('/', '.')
      in = end + 1
      ClassType(fromName(fullName))
    }

    def newMethodType: MethodType = {
      def getParamTypes(): List[Type] = {
        if (descriptor(in) == ')') Nil
        else getType() :: getParamTypes()
      }
      val params = getParamTypes()
      in += 1
      MethodType(params, getType())
    }

    getType()
  }

  override def toString = s"Definitions(classPath = ${classPath.asClassPathString})"
}
