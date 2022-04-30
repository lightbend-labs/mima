package com.typesafe.tools.mima.core

import TastyFormat._, TastyTagOps._
import TastyUnpickler._

object TastyPrinter {
  def printPickle(in: TastyReader, path: String): Unit = {
    println(s"unpickling $path")

    // val header =
    readHeader(in)
    // printHeader(header)

    val names = readNames(in)
    // printNames(names)

    printAllTrees(getTreeReader(in, names), names)
  }

  def printHeader(header: Header) = {
    val (h1, h2, h3, h4)                     = header.header
    val (versionMaj, versionMin, versionExp) = header.version
    println(s"Header:  ${List(h1, h2, h3, h4).map(_.toHexString.toUpperCase).mkString(" ")}")
    println(s"Version: $versionMaj.$versionMin-$versionExp")
    println(s"Tooling: ${header.toolingVersion}")
    println(s"UUID:    ${header.uuid.toString.toUpperCase}")
  }

  def printNames(names: Names) = {
    println("Names:")
    for ((name, idx) <- names.zipWithIndex)
      println(s"${nameStr(f"$idx%4d")}: ${name.debug}")
  }

  def printAllTrees(in: TastyReader, names: Names) = {
    import in._
    print(
      s"Trees: start=${startAddr.index} base=$base current=${currentAddr.index} end=${endAddr.index}; ${endAddr.index - startAddr.index} bytes of AST"
    )

    var indent      = 0
    def newLine()   = print(s"\n ${treeStr(f"${index(currentAddr) - index(startAddr)}%5d")}:" + " " * indent)
    def printNat()  = print(treeStr(" " + readNat()))
    def printName() = { val ref = readNat(); print(nameStr(s" $ref [${names(ref).debug}]")) }

    def printTree(): Unit = {
      newLine()
      indent += 2
      val tag = readByte()
      print(s" ${astTagToString(tag)}")

      def printLengthTree() = {
        val len          = readNat()
        val end          = currentAddr + len
        def printTrees() = doUntil(end)(printTree())
        def printMethodic() = {
          printTree()
          while (currentAddr.index < end.index && !isModifierTag(nextByte)) {
            printTree()
            printName()
          }
          printTrees()
        }

        print(s"(${lengthStr(len.toString)})")
        tag match {
          case VALDEF    => printName(); printTrees()
          case DEFDEF    => printName(); printTrees()
          case TYPEDEF   => printName(); printTrees()
          case TYPEPARAM => printName(); printTrees()
          case PARAM     => printName(); printTrees()

          case RETURN => printNat(); printTrees()

          case BIND        => printName(); printTrees()
          case REFINEDtype => printName(); printTrees()

          case POLYtype       => printMethodic()
          case TYPELAMBDAtype => printMethodic()

          case PARAMtype => printNat(); printNat() // target/ref Addr + paramNum

          case TERMREFin => printName(); printTrees()
          case TYPEREFin => printName(); printTrees()
          case SELECTin  => printName(); printTrees()

          case METHODtype => printMethodic()

          case _ => printTrees()
        }
        if (currentAddr != end) {
          print(s"incomplete read, current = $currentAddr, end = $end\n")
          goto(end)
        }
      }

      def printNatASTTree() = tag match {
        case TERMREFsymbol | TYPEREFsymbol => printNat(); printTree()
        case _                             => printName(); printTree()
      }

      def printNatTree() = tag match {
        case TERMREFpkg | TYPEREFpkg | STRINGconst | IMPORTED | RENAMED => printName()
        case _                                                          => printNat()
      }

      astCategory(tag) match {
        case AstCat1TagOnly =>
        case AstCat2Nat     => printNatTree()
        case AstCat3AST     => printTree()
        case AstCat4NatAST  => printNatASTTree()
        case AstCat5Length  => printLengthTree()
      }

      indent -= 2
    }

    while (!isAtEnd) printTree()
    println()
  }

  def printClassNames(in: TastyReader, path: String): Unit = {
    readHeader(in)
    val names      = readNames(in)
    val (pkg, nme) = unpicklePkgAndClsName(getTreeReader(in, names), names)
    println(s"${lengthStr(path)} -> ${treeStr(pkg.source)}.${nameStr(nme.source)}")
  }

  private def unpicklePkgAndClsName(in: TastyReader, names: Names): (Name, Name) = {
    import in._
    def readName(r: TastyReader = in) = names(r.readNat())
    def readNames(packageName: Name): (Name, Name) = readByte() match {
      case TYPEDEF    => readEnd(); (packageName, readName())
      case PACKAGE    => readEnd(); readNames(packageName)
      case TERMREFpkg => readNames(readName())
      case TYPEREFpkg => readNames(readName())
      case SHAREDtype => val r = forkAt(readAddr()); r.readByte(); readNames(readName(r))
      case t          => skipTreeTagged(in, t); readNames(packageName)
    }
    readNames(nme.Empty)
  }

  private def nameStr(str: String)   = Console.MAGENTA + str + Console.RESET
  private def treeStr(str: String)   = Console.YELLOW + str + Console.RESET
  private def lengthStr(str: String) = Console.CYAN + str + Console.RESET
}
