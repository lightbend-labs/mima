package com.typesafe.tools.mima.core

import TastyFormat._, NameTags._

object TastyTagOps {
  def nameTagToString(tag: Int) = tag match {
    case UTF8           => "UTF8"
    case QUALIFIED      => "QUALIFIED"
    case EXPANDED       => "EXPANDED"
    case EXPANDPREFIX   => "EXPANDPREFIX"
    case UNIQUE         => "UNIQUE"
    case DEFAULTGETTER  => "DEFAULTGETTER"
    case SUPERACCESSOR  => "SUPERACCESSOR"
    case INLINEACCESSOR => "INLINEACCESSOR"
    case BODYRETAINER   => "BODYRETAINER"
    case OBJECTCLASS    => "OBJECTCLASS"
    case SIGNED         => "SIGNED"
    case TARGETSIGNED   => "TARGETSIGNED"
    case id             => s"NotANameTag($id)"
  }

  def isModifierTag(tag: Int): Boolean = tag match {
    case PRIVATE            => true
    case PROTECTED          => true
    case ABSTRACT           => true
    case FINAL              => true
    case SEALED             => true
    case CASE               => true
    case IMPLICIT           => true
    case ERASED             => true
    case LAZY               => true
    case OVERRIDE           => true
    case INLINE             => true
    case INLINEPROXY        => true
    case MACRO              => true
    case OPAQUE             => true
    case STATIC             => true
    case OBJECT             => true
    case TRAIT              => true
    case ENUM               => true
    case LOCAL              => true
    case SYNTHETIC          => true
    case ARTIFACT           => true
    case MUTABLE            => true
    case FIELDaccessor      => true
    case  CASEaccessor      => true
    case COVARIANT          => true
    case CONTRAVARIANT      => true
    case HASDEFAULT         => true
    case STABLE             => true
    case EXTENSION          => true
    case GIVEN              => true
    case PARAMsetter        => true
    case EXPORTED           => true
    case OPEN               => true
    case PARAMalias         => true
    case TRANSPARENT        => true
    case INFIX              => true
    case INVISIBLE          => true
    case ANNOTATION         => true
    case   PRIVATEqualified => true
    case PROTECTEDqualified => true
    case _                  => false
  }

  def astTagToString(tag: Int): String = tag match {
    case  UNITconst    =>  "UNITconst"
    case FALSEconst    => "FALSEconst"
    case  TRUEconst    =>  "TRUEconst"
    case  NULLconst    =>  "NULLconst"
    case PRIVATE       => "PRIVATE"
    case PROTECTED     => "PROTECTED"
    case ABSTRACT      => "ABSTRACT"
    case FINAL         => "FINAL"
    case SEALED        => "SEALED"
    case CASE          => "CASE"
    case IMPLICIT      => "IMPLICIT"
    case ERASED        => "ERASED"
    case LAZY          => "LAZY"
    case OVERRIDE      => "OVERRIDE"
    case INLINE        => "INLINE"
    case INLINEPROXY   => "INLINEPROXY"
    case MACRO         => "MACRO"
    case OPAQUE        => "OPAQUE"
    case STATIC        => "STATIC"
    case OBJECT        => "OBJECT"
    case TRAIT         => "TRAIT"
    case ENUM          => "ENUM"
    case LOCAL         => "LOCAL"
    case SYNTHETIC     => "SYNTHETIC"
    case ARTIFACT      => "ARTIFACT"
    case MUTABLE       => "MUTABLE"
    case FIELDaccessor => "FIELDaccessor"
    case  CASEaccessor =>  "CASEaccessor"
    case COVARIANT     => "COVARIANT"
    case CONTRAVARIANT => "CONTRAVARIANT"
    case HASDEFAULT    => "HASDEFAULT"
    case STABLE        => "STABLE"
    case EXTENSION     => "EXTENSION"
    case GIVEN         => "GIVEN"
    case PARAMsetter   => "PARAMsetter"
    case EXPORTED      => "EXPORTED"
    case OPEN          => "OPEN"
    case PARAMalias    => "PARAMalias"
    case TRANSPARENT   => "TRANSPARENT"
    case INFIX         => "INFIX"
    case INVISIBLE     => "INVISIBLE"
    case EMPTYCLAUSE   => "EMPTYCLAUSE"
    case SPLITCLAUSE   => "SPLITCLAUSE"

    case SHAREDterm    => "SHAREDterm"
    case SHAREDtype    => "SHAREDtype"
    case TERMREFdirect => "TERMREFdirect"
    case TYPEREFdirect => "TYPEREFdirect"
    case TERMREFpkg    => "TERMREFpkg"
    case TYPEREFpkg    => "TYPEREFpkg"
    case RECthis       => "RECthis"
    case   BYTEconst   =>   "BYTEconst"
    case  SHORTconst   =>  "SHORTconst"
    case   CHARconst   =>   "CHARconst"
    case    INTconst   =>    "INTconst"
    case   LONGconst   =>   "LONGconst"
    case  FLOATconst   =>  "FLOATconst"
    case DOUBLEconst   => "DOUBLEconst"
    case STRINGconst   => "STRINGconst"
    case IMPORTED      => "IMPORTED"
    case RENAMED       => "RENAMED"

    case THIS               => "THIS"
    case QUALTHIS           => "QUALTHIS"
    case CLASSconst         => "CLASSconst"
    case BYNAMEtype         => "BYNAMEtype"
    case BYNAMEtpt          => "BYNAMEtpt"
    case NEW                => "NEW"
    case THROW              => "THROW"
    case IMPLICITarg        => "IMPLICITarg"
    case   PRIVATEqualified =>   "PRIVATEqualified"
    case PROTECTEDqualified => "PROTECTEDqualified"
    case RECtype            => "RECtype"
    case SINGLETONtpt       => "SINGLETONtpt"
    case BOUNDED            => "BOUNDED"

    case IDENT         => "IDENT"
    case IDENTtpt      => "IDENTtpt"
    case SELECT        => "SELECT"
    case SELECTtpt     => "SELECTtpt"
    case TERMREFsymbol => "TERMREFsymbol"
    case TERMREF       => "TERMREF"
    case TYPEREFsymbol => "TYPEREFsymbol"
    case TYPEREF       => "TYPEREF"
    case SELFDEF       => "SELFDEF"
    case NAMEDARG      => "NAMEDARG"

    case PACKAGE        => "PACKAGE"
    case VALDEF         => "VALDEF"
    case DEFDEF         => "DEFDEF"
    case TYPEDEF        => "TYPEDEF"
    case IMPORT         => "IMPORT"
    case TYPEPARAM      => "TYPEPARAM"
    case PARAM          => "PARAM"
    case APPLY          => "APPLY"
    case TYPEAPPLY      => "TYPEAPPLY"
    case TYPED          => "TYPED"
    case ASSIGN         => "ASSIGN"
    case BLOCK          => "BLOCK"
    case IF             => "IF"
    case LAMBDA         => "LAMBDA"
    case MATCH          => "MATCH"
    case RETURN         => "RETURN"
    case WHILE          => "WHILE"
    case TRY            => "TRY"
    case INLINED        => "INLINED"
    case SELECTouter    => "SELECTouter"
    case REPEATED       => "REPEATED"
    case BIND           => "BIND"
    case ALTERNATIVE    => "ALTERNATIVE"
    case UNAPPLY        => "UNAPPLY"
    case ANNOTATEDtype  => "ANNOTATEDtype"
    case ANNOTATEDtpt   => "ANNOTATEDtpt"
    case CASEDEF        => "CASEDEF"
    case TEMPLATE       => "TEMPLATE"
    case SUPER          => "SUPER"
    case SUPERtype      => "SUPERtype"
    case REFINEDtype    => "REFINEDtype"
    case REFINEDtpt     => "REFINEDtpt"
    case APPLIEDtype    => "APPLIEDtype"
    case APPLIEDtpt     => "APPLIEDtpt"
    case TYPEBOUNDS     => "TYPEBOUNDS"
    case TYPEBOUNDStpt  => "TYPEBOUNDStpt"
    case        ANDtype =>        "ANDtype"
    case         ORtype =>         "ORtype"
    case       POLYtype =>       "POLYtype"
    case TYPELAMBDAtype => "TYPELAMBDAtype"
    case     LAMBDAtpt  =>     "LAMBDAtpt"
    case      PARAMtype =>      "PARAMtype"
    case ANNOTATION     => "ANNOTATION"
    case TERMREFin      => "TERMREFin"
    case TYPEREFin      => "TYPEREFin"
    case  SELECTin      =>  "SELECTin"
    case EXPORT         => "EXPORT"
    case METHODtype     => "METHODtype"
    case MATCHtype      => "MATCHtype"
    case MATCHtpt       => "MATCHtpt"
    case MATCHCASEtype  => "MATCHCASEtype"
    case HOLE           => "HOLE"

    case tag => s"BadTag($tag)"
  }

  sealed abstract class AstCategory(val range: Range.Inclusive)
  case object AstCat1TagOnly extends AstCategory(AstCat1)
  case object AstCat2Nat     extends AstCategory(AstCat2)
  case object AstCat3AST     extends AstCategory(AstCat3)
  case object AstCat4NatAST  extends AstCategory(AstCat4)
  case object AstCat5Length  extends AstCategory(AstCat5)
  val astCategories = List(AstCat1TagOnly, AstCat2Nat, AstCat3AST, AstCat4NatAST, AstCat5Length)
  def astCategory(tag: Int): AstCategory = astCategories.iterator.filter(_.range.contains(tag)).next()
}
