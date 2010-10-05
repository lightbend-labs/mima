package ssol.tools.mima

import java.io._
import java.util.zip._
import collection.mutable

object JarCopy {
  val BUFFER = 2 << 14

  var entries = new mutable.ListBuffer[String]

  type Patches = mutable.HashMap[String, Array[Byte]]

  def copy(from: String, to: String, patches: Patches) {
    val fis = new FileInputStream(from)
    val zis = new ZipInputStream(new BufferedInputStream(fis))
    val fos = new FileOutputStream(to)
    val zos = new ZipOutputStream(new BufferedOutputStream(fos))    
    patch(zis, zos, patches)
  }

  def patch(zis: ZipInputStream, zos: ZipOutputStream, patches: Patches) {
    val buf = new Array[Byte](2 << 16)

    def addPatch(ename: String, data: Array[Byte]) {
      val outEntry = new ZipEntry(ename)
      outEntry.setSize(data.length)
      val crc = new Adler32
      crc update data
      outEntry.setCrc(crc.getValue)
      zos.putNextEntry(outEntry)
      zos.write(data, 0, data.length)
      patches -= ename
    }

    def writePatch(data: Array[Byte], outEntry: ZipEntry) {
      println("Patching: " +outEntry+":"+outEntry.getSize+" "+outEntry.getTime+" "+outEntry.getCrc+" "+outEntry.getMethod)
      outEntry.setSize(data.size)
      outEntry.setCompressedSize(-1)
      zos.putNextEntry(outEntry)
      zos.write(data, 0, data.length)
      patches -= outEntry.getName
    }

    def copyEntry(entry: ZipEntry, outEntry: ZipEntry) {
      println("Copying: " +entry+":"+entry.getSize+" "+entry.getTime+" "+entry.getCrc+" "+entry.getMethod)
      zos.putNextEntry(outEntry)
      var count = zis.read(buf, 0, BUFFER)
      while (count != -1) {
        zos.write(buf, 0, count)
        count = zis.read(buf, 0, BUFFER)
      }
    }

    var entry = zis.getNextEntry()
    while (entry != null) {
      val outEntry = new ZipEntry(entry)
      patches get entry.getName match {
        case Some(data) => 
          writePatch(data, outEntry)
        case None =>
          copyEntry(entry, outEntry)
      }
      entry = zis.getNextEntry()
    }
    zis.close()
    for ((ename, data) <- patches) 
      addPatch(ename, data)
    zos.close()
  }

  def main(args: Array[String]) {
    copy(args(0), args(1), new Patches)
  }
}
