package com.typesafe.tools.mima.core


/** This class holds together a root package and a classpath. It
 *  also offers definitions of commonly used classes, including
 *  java.lang.Object
 *
 *  Each version of the input jar file has an instance of Definitions, used
 *  to resolve type names during classfile parsing.
 */
class Definitions(val cp: CompilerClassPath, val classPath: CompilerClassPath) {
  import com.typesafe.tools.mima.core.util.log.ConsoleLogging._

  lazy val root = definitionsPackageInfo(this)

  /** Return all packages in the target library. */
  lazy val targetPackage: PackageInfo = {
    val pkg = new SyntheticPackageInfo(root, "<root>") {
      override def isRoot = true
      /** Needed to fetch classes located in the root (empty package) */
      override lazy val classes = Definitions.this.root.classes
    }
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

  /** Return the type corresponding to 'sig'. Class names are resolved
   *  relative to the current classpath.
   */
  def fromSig(sig: String): Type = {
    var in = 0

    def getType(): Type = {
      val ch = sig(in)
      in += 1
      abbrevToValueType get ch match {
        case Some(tp) =>
          tp
        case None =>
          if (ch == '[') {
            ArrayType(getType())
          } else if (ch == 'L') {
            val end = sig indexOf (';', in)
            val fullname = sig.substring(in, end) replace ('/', '.')
            in = end + 1
            ClassType(fromName(fullname))
          } else if (ch == '(') {
            val params = getParamTypes()
            in += 1
            MethodType(params, getType())
          } else {
            throw new MatchError("unknown signature: "+sig.substring(in))
          }
      }
    }

    def getParamTypes(): List[Type] =
      if (sig(in) == ')') List()
      else getType() :: getParamTypes()

    getType()
  }

  override def toString = {
    "definitions:\n\tcp: %s\n%s".format(cp, asClassPathString(classPath))
  }
}
