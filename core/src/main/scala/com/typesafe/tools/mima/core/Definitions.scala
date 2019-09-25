package com.typesafe.tools.mima.core

import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.symtab.classfile.ClassfileConstants._

/** This class holds together a root package and a classpath. It
 *  also offers definitions of commonly used classes, including
 *  java.lang.Object
 *
 *  Each version of the input jar file has an instance of Definitions, used
 *  to resolve type names during classfile parsing.
 */
class Definitions(val lib: Option[ClassPath], val classPath: ClassPath) {
  import com.typesafe.tools.mima.core.util.log.ConsoleLogging._

  lazy val root = definitionsPackageInfo(this)

  /** Return all packages in the target library. */
  lazy val targetPackage: PackageInfo = {
    val pkg = new DefinitionsTargetPackageInfo(root)
    val cp = lib.getOrElse(AggregateClassPath.createAggregate())
    pkg.packages ++= definitionsTargetPackages(pkg, cp, this)
    debugLog("added packages to <root>: %s".format(pkg.packages.keys.mkString(", ")))
    pkg
  }

  lazy val ObjectClass = fromName("java.lang.Object")
  lazy val AnnotationClass = fromName("java.lang.annotation.Annotation")

  /** Return the class corresponding to the fully qualified name.
   *  If there is no such class in the current classpath, a SyntheticClassInfo
   *  and all necessary SyntheticPackageInfo are created along the way.
   */
  def fromName(name: String): ClassInfo = {
    val parts = name.split("\\.")
    var pkg: PackageInfo = this.root
    var i = 0
    var part = parts(i)
    while (i < parts.length - 1) {
      pkg.packages get part match {
        case Some(p) => pkg = p
        case None =>
          val newpkg = new SyntheticPackageInfo(pkg, part)
          pkg.packages += (part -> newpkg)
          pkg = newpkg
      }
      i += 1
      part = parts(i)
    }
    pkg.classes getOrElse (part, new SyntheticClassInfo(pkg, part))
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

  override def toString = {
    "definitions:\n\tlib: %s\n%s".format(lib, classPath.asClassPathString)
  }
}
