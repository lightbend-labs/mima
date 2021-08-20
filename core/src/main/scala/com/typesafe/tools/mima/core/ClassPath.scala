package com.typesafe.tools.mima.core

import java.nio.file._

import scala.collection.JavaConverters._

private object AbsFile {
  def apply(p: Path): AbsFile = {
    val name = p.getFileName.toString.stripPrefix("/")
    val path = p.toString.stripPrefix("/")
    AbsFile(name)(path, () => Files.readAllBytes(p))
  }
}

private[core] final case class AbsFile(name: String)(val path: String, bytes: () => Array[Byte]) {
  // Not defined as a simple wrapper of java.nio.file.Path, because Path#equals uses its FileSystem,
  // differently to scala-reflect's AbstractFile, which breaks things like `distinct`.

  def toByteArray       = bytes()
  override def toString = path
}

private[mima] sealed trait ClassPath extends Product with Serializable {
  def packages(pkg: String): Stream[String]
  def  classes(pkg: String): Stream[AbsFile]
  def asClassPathString: String
}

private[mima] object ClassPath {
  val RootPackage     = ""
  val base: ClassPath = of(jrt +: javaBootCp)

  def of(xs: Seq[ClassPath]): ClassPath = {
    xs.toStream.flatMap {
      case x: AggrCp => x.aggregates
      case x                     => List(x)
    } match {
      case Seq(x) => x
      case xs     => AggrCp(xs)
    }
  }

  def fromJarOrDir(file: java.io.File): Option[ClassPath] = Option(file.toPath).collect {
    case p if file.isDirectory                             => PathCp(p)(p)
    case p if file.isFile && file.getName.endsWith(".jar") => PathCp(p)(rootPath(p))
  }

          def join(xs: Seq[String]) = xs.filter("" != _).mkString(java.io.File.pathSeparator)
          def split(cp: String)     = cp.split(java.io.File.pathSeparator).toStream.filter("" != _).distinct
  private def expandCp(cp: String)  = split(cp).flatMap(s => fromJarOrDir(new java.io.File(s)))
  private def javaBootCp            = expandCp(System.getProperty("sun.boot.class.path", ""))

  private def list(p: Path)      = andClose(Files.newDirectoryStream(p))(_.asScala.toStream.sortBy(_.toString))
  private def listDir(p: Path)   = if (Files.isDirectory(p)) list(p) else Stream.empty
  private def readLink(p: Path)  = if (Files.isSymbolicLink(p)) Files.readSymbolicLink(p) else p
  private def rootPath(p: Path)  = FileSystems.newFileSystem(p, null: ClassLoader).getPath("/")
  private def isClass(p: Path)   = p.getFileName.toString.endsWith(".class") && Files.isRegularFile(p)
  private def isPackage(p: Path) = p.getFileName.toString.stripSuffix("/") match {
    case "META-INF" | "" => false
    case dirName         => dirName.charAt(0) != '.' && Files.isDirectory(p)
  }

  private def optSuf(s: String, sep: String)        = if (s.isEmpty) "" else s"$s$sep"
  private def pkgToDirPath(pkg: String)             = optSuf(pkg.replace('.', '/'), "/")
  private def pkgResolve(p: Path, pkg: String)      = p.resolve(pkgToDirPath(pkg))
  private def pkgEntry(pkg: String, p: Path)        = optSuf(pkg, ".") + p.getFileName.toString.stripSuffix("/")
  private def pkgClasses(m: Path, pkg: String)      = listDir(pkgResolve(m, pkg)).filter(isClass)
  private def pkgContains(pkg: String, sub: String) =
    pkg.isEmpty || sub.startsWith(pkg) && sub.lastIndexOf(".") == pkg.length

  private def jrt = if (!scala.util.Properties.isJavaAtLeast("9")) of(Nil) else
    try JrtCp(FileSystems.getFileSystem(java.net.URI.create("jrt:/")))
    catch { case _: ProviderNotFoundException | _: FileSystemNotFoundException => of(Nil) }

  private final case class JrtCp(fs: FileSystem) extends ClassPath {
    def packages(pkg: String) = packageToModules.keys.toStream.filter(pkgContains(pkg, _)).sorted
    def  classes(pkg: String) = packageToModules(pkg).flatMap(pkgClasses(_, pkg)).sortBy(_.toString).map(AbsFile(_))
    def asClassPathString     = fs.toString

    private val packageToModules = listDir(fs.getPath("/packages"))
      .map(p => p.toString.stripPrefix("/packages/") -> listDir(p).map(readLink))
      .toMap.withDefaultValue(Stream.empty)
  }

  private final case class PathCp(src: Path)(root: Path) extends ClassPath {
    def packages(pkg: String) = listDir(pkgResolve(root, pkg)).filter(isPackage).map(pkgEntry(pkg, _))
    def  classes(pkg: String) = listDir(pkgResolve(root, pkg)).filter(isClass).map(AbsFile(_))
    def asClassPathString     = src.toString
  }

  private final case class AggrCp(aggregates: Stream[ClassPath]) extends ClassPath {
    def packages(pkg: String) = aggregates.flatMap(_.packages(pkg)).distinct
    def  classes(pkg: String) = aggregates.flatMap(_.classes(pkg)).distinct
    def asClassPathString     = join(aggregates.map(_.asClassPathString).distinct)
  }

  private def andClose[A <: AutoCloseable, B](xs: A)(f: A => B): B = try f(xs) finally xs.close()
}
