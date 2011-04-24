package ssol.tools
package mima

import scala.tools.nsc.io.{ AbstractFile, PlainFile, ZipArchive }
import java.io._
import java.util.zip._
import collection.mutable


class Writer(fixes: Seq[Fix], config: WriterConfig) {

  /** All class files that belongs in a zip for which a `Fix` exists */
  private def zippedClassFiles: Seq[AbstractFile] = fixes.map(_.clazz.file).filter(_.isInstanceOf[ZipArchive#FileEntry])

  /** get the zip container of the provided `entry` file */
  private def archive(entry: AbstractFile): ZipArchive = entry match {
    case fe: ZipArchive#Entry => archive(entry.container)
    case ar: ZipArchive       => ar
  }

  /** jar files with classes needing fixes */
  private lazy val jarfiles: Seq[ZipArchive] = zippedClassFiles.map(archive).distinct

  private def writeTo(file: File)(op: File => Unit) {
    val out = new File(file.getAbsolutePath+".0")
    op(out)
    
    if (out.renameTo(file)) {
      println(out+" was correctly patched")
    } else {
      println("*** cannot rename " + out + " to " + file + ";\n" +
        "leaving patched file in " + out)
    }
  }

  def writeOut() {
    if (jarfiles.isEmpty)  {
      println("Nothing to fix.")
      return
    }
    
    println("[jars to fix: " + jarfiles.mkString(", ") + "]")
      
    val jarPatches: Map[ZipArchive, JarCopy.Patches] = (jarfiles map (_ -> new JarCopy.Patches)).toMap
    for (fix <- fixes) {
      val data = fix.outputStream.asInstanceOf[ByteArrayOutputStream].toByteArray
      fix.clazz.file match {
        case ze: ZipArchive#FileEntry =>
          jarPatches(archive(ze)) += ze.entry.getName -> data
        case pf: PlainFile =>
          assert(false, "never here") //FIXME: Why is this needed in the first place?
          writeTo(pf.file) { f =>
            val os = new FileOutputStream(f)
            os.write(data)
            os.close()
          }
      }
    }
    for ((jarfile, patches) <- jarPatches) {
      val fis = new FileInputStream(jarfile.file)
      val zis = new ZipInputStream(new BufferedInputStream(fis))
      
      val targetJarFile = config.rename(jarfile.file)
      
      writeTo(targetJarFile) { f =>
        val fos = new FileOutputStream(f)
        val zos = new ZipOutputStream(new BufferedOutputStream(fos))
        JarCopy.patch(zis, zos, patches)
      }
    }
    for (fix <- fixes) {
      Config.info("[" + "Fixed up " + fix.clazz + " in " + fix.outputFileName + "]")
    }
    
    val fixesStr = fixes.length match {
      case 0 => "no classes to fix"
      case 1 => "one class fixed"
      case n => n + " classes fixed"
    }
    println("[" + ClassfileParser.parsed + " classes parsed, " + fixesStr + "]")
  }
}
