package ssol.tools
package mima

import scala.tools.nsc.io.{ AbstractFile, PlainFile, ZipArchive }
import java.io._
import java.util.zip._
import collection.mutable

class Writer(fixes: Seq[Fix]) {

  private def zippedClassFiles = fixes.map(_.clazz.file).filter(_.isInstanceOf[ZipArchive#FileEntry])

  private def archive(entry: AbstractFile): ZipArchive = entry match {
    case fe: ZipArchive#Entry => archive(entry.container)
    case ar: ZipArchive       => ar
  }

  private lazy val jarfiles = zippedClassFiles.map(archive).distinct

  private def overwrite(file: File)(op: File => Unit) {
    val out = new File(file.getAbsolutePath + ".0")
    op(out)
    val backup = new File(file.getAbsolutePath + ".1")
    backup.delete()
    if (file.renameTo(backup)) {
      if (out.renameTo(file))
        backup.delete()
      else {
        println("*** cannot rename patched " + out + " to original " + file + ";\n" +
          "leaving patched file in " + out)
        backup.renameTo(file)
      }
    } else {
      println("*** cannot rename " + file + " to " + backup + ";\n" +
        "leaving patched file in " + out)
    }
  }

  private var fixesStr = ""

  def writeOut() {
    if (Config.inPlace && jarfiles.nonEmpty) {
      println("[jars to fix: " + jarfiles.mkString(", ") + "]")
    }
    val jarPatches = (jarfiles map (_ -> new JarCopy.Patches)).toMap
    for (fix <- fixes) {
      val data = fix.outputStream.asInstanceOf[ByteArrayOutputStream].toByteArray
      fix.clazz.file match {
        case ze: ZipArchive#FileEntry =>
          jarPatches(archive(ze)) += ze.entry.getName -> data
        case pf: PlainFile =>
          overwrite(pf.file) { f =>
            val os = new FileOutputStream(f)
            os.write(data)
            os.close()
          }
      }
    }
    for ((jarfile, patches) <- jarPatches) {
      val fis = new FileInputStream(jarfile.file)
      val zis = new ZipInputStream(new BufferedInputStream(fis))
      overwrite(jarfile.file) { f =>
        val fos = new FileOutputStream(f)
        val zos = new ZipOutputStream(new BufferedOutputStream(fos))
        JarCopy.patch(zis, zos, patches)
      }
    }
    for (fix <- fixes) {
      Config.info("[" + "Fixed up " + fix.clazz + " in " + fix.outputFileName + "]")
    }
    fixesStr = fixes.length match {
      case 0 => "no classes to fix"
      case 1 => "one class fixed"
      case n => n + " classes fixed"
    }
    println("[" + ClassfileParser.parsed + " classes parsed, " + fixesStr + "]")
  }
}
