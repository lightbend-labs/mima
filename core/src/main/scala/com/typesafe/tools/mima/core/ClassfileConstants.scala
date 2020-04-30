package com.typesafe.tools.mima.core

private[core] object ClassfileConstants {
  final val JAVA_MAGIC = 0xCAFEBABE

  final val JAVA_ACC_PUBLIC       = 0x0001
  final val JAVA_ACC_PRIVATE      = 0x0002
  final val JAVA_ACC_PROTECTED    = 0x0004
  final val JAVA_ACC_STATIC       = 0x0008
  final val JAVA_ACC_FINAL        = 0x0010
  final val JAVA_ACC_INTERFACE    = 0x0200
  final val JAVA_ACC_ABSTRACT     = 0x0400
  final val JAVA_ACC_SYNTHETIC    = 0x1000
  final val JAVA_ACC_ANNOTATION   = 0x2000

  final val CONSTANT_UTF8          =  1
  final val CONSTANT_UNICODE       =  2
  final val CONSTANT_INTEGER       =  3
  final val CONSTANT_FLOAT         =  4
  final val CONSTANT_LONG          =  5
  final val CONSTANT_DOUBLE        =  6
  final val CONSTANT_CLASS         =  7
  final val CONSTANT_STRING        =  8
  final val CONSTANT_FIELDREF      =  9
  final val CONSTANT_METHODREF     = 10
  final val CONSTANT_INTFMETHODREF = 11
  final val CONSTANT_NAMEANDTYPE   = 12
  final val CONSTANT_METHODHANDLE  = 15
  final val CONSTANT_METHODTYPE    = 16
  final val CONSTANT_INVOKEDYNAMIC = 18
  final val CONSTANT_MODULE        = 19
  final val CONSTANT_PACKAGE       = 20

  final val BYTE_TAG       = 'B'
  final val CHAR_TAG       = 'C'
  final val DOUBLE_TAG     = 'D'
  final val FLOAT_TAG      = 'F'
  final val INT_TAG        = 'I'
  final val LONG_TAG       = 'J'
  final val SHORT_TAG      = 'S'
  final val BOOL_TAG       = 'Z'
  final val ARRAY_TAG      = '['
  final val VOID_TAG       = 'V'
  final val OBJECT_TAG     = 'L'
  final val ANNOTATION_TAG = '@'
}
