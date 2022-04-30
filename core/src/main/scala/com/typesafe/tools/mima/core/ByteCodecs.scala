package com.typesafe.tools.mima.core

/**
 * Helper methods to serialize a byte array as String that can be written as "modified" UTF-8 to classfiles.
 *
 * Modified UTF-8 is the same as UTF-8, except for 0x00, which is represented as the "overlong" 0xC0 0x80. Constant
 * strings in classfiles use this encoding.
 *
 * Encoding (according to SID-10):
 *   - The 8-bit bytes are split into 7-bit bytes, e.g., 0xff 0x0f becomes 0x7f 0x1f 0x00
 *   - Every bit is incremented by 1 (modulo 0x80), in the example we get 0x00, 0x20 0x01
 *   - 0x00 is mapped to the overlong encoding, so we get 0xC0 0x80 0x20 0x01
 *
 * The +1 increment should reduce the number of (overlong) zeros in the resulting string, as 0x7f is (hoped to be) more
 * common than 0x00.
 */
object ByteCodecs {

  /** Map 0xC0 0x80 to 0x00, then subtract 1 from each element. In-place. */
  def regenerateZero(src: Array[Byte]): Int = {
    var i      = 0
    val srclen = src.length
    var j      = 0
    while (i < srclen) {
      val in: Int = src(i) & 0xff
      if (in == 0xc0 && (src(i + 1) & 0xff) == 0x80) {
        src(j) = 0x7f
        i += 2
      } else if (in == 0) {
        src(j) = 0x7f
        i += 1
      } else {
        src(j) = (in - 1).toByte
        i += 1
      }
      j += 1
    }
    j
  }

  def decode7to8(src: Array[Byte], srclen: Int): Int = {
    var i      = 0
    var j      = 0
    val dstlen = (srclen * 7 + 7) / 8
    while (i + 7 < srclen) {
      var out: Int = src(i).toInt
      var in: Byte = src(i + 1)
      src(j) = (out | (in & 0x01) << 7).toByte
      out = in >>> 1
      in = src(i + 2)
      src(j + 1) = (out | (in & 0x03) << 6).toByte
      out = in >>> 2
      in = src(i + 3)
      src(j + 2) = (out | (in & 0x07) << 5).toByte
      out = in >>> 3
      in = src(i + 4)
      src(j + 3) = (out | (in & 0x0f) << 4).toByte
      out = in >>> 4
      in = src(i + 5)
      src(j + 4) = (out | (in & 0x1f) << 3).toByte
      out = in >>> 5
      in = src(i + 6)
      src(j + 5) = (out | (in & 0x3f) << 2).toByte
      out = in >>> 6
      in = src(i + 7)
      src(j + 6) = (out | in << 1).toByte
      i += 8
      j += 7
    }
    if (i < srclen) {
      var out: Int = src(i).toInt
      if (i + 1 < srclen) {
        var in: Byte = src(i + 1)
        src(j) = (out | (in & 0x01) << 7).toByte; j += 1
        out = in >>> 1
        if (i + 2 < srclen) {
          in = src(i + 2)
          src(j) = (out | (in & 0x03) << 6).toByte; j += 1
          out = in >>> 2
          if (i + 3 < srclen) {
            in = src(i + 3)
            src(j) = (out | (in & 0x07) << 5).toByte; j += 1
            out = in >>> 3
            if (i + 4 < srclen) {
              in = src(i + 4)
              src(j) = (out | (in & 0x0f) << 4).toByte; j += 1
              out = in >>> 4
              if (i + 5 < srclen) {
                in = src(i + 5)
                src(j) = (out | (in & 0x1f) << 3).toByte; j += 1
                out = in >>> 5
                if (i + 6 < srclen) {
                  in = src(i + 6)
                  src(j) = (out | (in & 0x3f) << 2).toByte; j += 1
                  out = in >>> 6
                }
              }
            }
          }
        }
      }
      if (j < dstlen) src(j) = out.toByte
    }
    dstlen
  }

  /**
   * Destructively decodes array xs and returns the length of the decoded array.
   *
   * Sometimes returns (length+1) of the decoded array. Example:
   *
   * scala> val enc = scala.reflect.internal.pickling.ByteCodecs.encode(Array(1,2,3)) enc: Array[Byte] = Array(2, 5, 13,
   * 1)
   *
   * scala> scala.reflect.internal.pickling.ByteCodecs.decode(enc) res43: Int = 4
   *
   * scala> enc res44: Array[Byte] = Array(1, 2, 3, 0)
   *
   * However, this does not always happen.
   */
  def decode(xs: Array[Byte]): Int = decode7to8(xs, regenerateZero(xs))
}
