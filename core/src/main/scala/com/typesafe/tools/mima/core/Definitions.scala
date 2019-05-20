package com.typesafe.tools.mima.core

import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.util.ClassPath

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

  lazy val ClassfileParser = new LibClassfileParser(this)

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

  import Type._

  /** Return the type corresponding to this descriptor. Class names are resolved
   *  relative to the current classpath.
   */
  def fromDescriptor(descriptor: String): Type = {
    var in = 0

    def getType(): Type = {
      val ch = descriptor(in)
      in += 1
      abbrevToValueType get ch match {
        case Some(tp) =>
          tp
        case None =>
          if (ch == '[') {
            ArrayType(getType())
          } else if (ch == 'L') {
            val end = descriptor indexOf (';', in)
            val fullname = descriptor.substring(in, end) replace ('/', '.')
            in = end + 1
            ClassType(fromName(fullname))
          } else if (ch == '(') {
            val params = getParamTypes()
            in += 1
            MethodType(params, getType())
          } else {
            throw new MatchError("unknown signature: "+descriptor.substring(in))
          }
      }
    }

    def getParamTypes(): List[Type] =
      if (descriptor(in) == ')') List()
      else getType() :: getParamTypes()

    getType()
  }
  @deprecated("Replaced by fromDescriptor", "0.3.1")
  def fromSig(sig: String): Type = fromDescriptor(sig)

  override def toString = {
    "definitions:\n\tlib: %s\n%s".format(lib, asClassPathString(classPath))
  }
}
