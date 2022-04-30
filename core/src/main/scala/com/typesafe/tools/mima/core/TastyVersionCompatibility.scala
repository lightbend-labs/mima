package com.typesafe.tools.mima.core

import TastyFormat._

object TastyVersionCompatibility {

  /**
   * This method implements a binary relation (`<:<`) between two TASTy versions.
   *
   * Given the TASTy version of a `file` and the TASTy version of the `compiler`, if `file <:< compiler` then the TASTy
   * file is valid to be read.
   *
   * A TASTy version, e.g. `v := 28.0-3` is composed of three fields:
   *   - v.major == 28
   *   - v.minor == 0
   *   - v.experimental == 3
   *
   * TASTy versions have a partial order, for example, `a <:< b` and `b <:< a` are both false if
   *   - `a` and `b` have different `major` fields.
   *   - `a` and `b` have the same `major` & `minor` fields, but different, non-zero, `experimental` fields.
   *
   * A TASTy version with a zero value for its `experimental` field is considered to be stable. Files with a stable
   * TASTy version can be read by a compiler with an unstable TASTy version, (where the compiler's TASTy version has a
   * higher `minor` field).
   *
   * A compiler with a stable TASTy version can never read a file with an unstable TASTy version.
   *
   * We follow the given algorithm:
   *
   * ```
   * file match
   *   case (MajorVersion, MinorVersion, ExperimentalVersion) => true // full equality
   *   case (MajorVersion, minor, 0) if minor < MinorVersion  => true // stable backwards compatibility
   *   case _                                                 => false
   * ```
   */
  def isVersionCompatible(fileMajor: Int, fileMinor: Int, fileExperimental: Int): Boolean =
    fileMajor == MajorVersion &&
    (fileMinor == MinorVersion && fileExperimental == ExperimentalVersion // full equality
    || fileMinor < MinorVersion && fileExperimental == 0 // stable backwards compatibility
    )
}
