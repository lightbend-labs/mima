package ssol.tools.mima

import java.io.File

class WriterConfig(val outDir: File, private val qualifier: String) {
  assert(outDir.isDirectory)
  assert(qualifier != null & !qualifier.trim.isEmpty)
  
  @throws(classOf[IllegalArgumentException])
  def rename(jarFile: File): File = {
    if(jarFile == null) throw new IllegalArgumentException("null is not a valid argument for `jarFile`")
    if(!jarFile.isFile) throw new IllegalArgumentException("Expected a file")
    if(!jarFile.getName.endsWith(".jar")) throw new IllegalArgumentException("Expected jar file, found " + jarFile.getName)
    
    val name = jarFile.getName.stripSuffix(".jar") + "-" + qualifier + ".jar"
    new File(outDir, name)
  }
}