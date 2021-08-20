package com.typesafe.tools.mima.core

object PickleFormat {

  /** Symbol table attribute format:
   *   Symtab         = nentries_Nat {Entry}
   *
   *         NameInfo     = <character sequence of length len_Nat in UTF-8 format>
   *       SymbolInfo     = name_Ref owner_Ref flags_LongNat [privateWithin_Ref] info_Ref
   *             Long     = <len_Nat-byte signed number in big endian format>
   *             Ref      = Nat
   *        AnnotInfoBody = info_Ref {annotArg_Ref} {name_Ref constAnnotArg_Ref}
   *        AnnotArg      = Tree | Constant
   *   ConstAnnotArg      = Constant | AnnotInfo | AnnotArgArray
   *
   *   len is remaining length after `len`.
   */

  /** Entry = \
   *     1 TERMNAME len_Nat NameInfo
   *   | 2 TYPENAME len_Nat NameInfo
   *   |
   *   | 3   NONEsym len_Nat
   *   | 4   TYPEsym len_Nat SymbolInfo
   *   | 5  ALIASsym len_Nat SymbolInfo
   *   | 6  CLASSsym len_Nat SymbolInfo [thistype_Ref]
   *   | 7 MODULEsym len_Nat SymbolInfo
   *   | 8    VALsym len_Nat [defaultGetter_Ref] SymbolInfo [alias_Ref]  // defaultGetter_Ref no longer needed
   *   |
   *   | 9          EXTref len_Nat name_Ref [owner_Ref]
   *   | 10 EXTMODCLASSref len_Nat name_Ref [owner_Ref]
   *   |
   *   | 11             NOtpe len_Nat
   *   | 12       NOPREFIXtpe len_Nat
   *   | 13           THIStpe len_Nat      sym_Ref
   *   | 14         SINGLEtpe len_Nat     type_Ref  sym_Ref
   *   | 15       CONSTANTtpe len_Nat constant_Ref
   *   | 16        TYPEREFtpe len_Nat     type_Ref  sym_Ref {targ_Ref}
   *   | 17     TYPEBOUNDStpe len_Nat      tpe_Ref  tpe_Ref
   *   | 18        REFINEDtpe len_Nat classsym_Ref {tpe_Ref}
   *   | 19      CLASSINFOtpe len_Nat classsym_Ref {tpe_Ref}
   *   | 20         METHODtpe len_Nat      tpe_Ref {sym_Ref}
   *   | 21          POLYTtpe len_Nat      tpe_Ref {sym_Ref}
   *   | 22 IMPLICITMETHODtpe len_Nat      tpe_Ref {sym_Ref}   // deprecated
   *   | 52          SUPERtpe len_Nat      tpe_Ref  tpe_Ref
   *   |
   *   | 24 LITERALunit    len_Nat
   *   | 25 LITERALboolean len_Nat value_Long
   *   | 26 LITERALbyte    len_Nat value_Long
   *   | 27 LITERALshort   len_Nat value_Long
   *   | 28 LITERALchar    len_Nat value_Long
   *   | 29 LITERALint     len_Nat value_Long
   *   | 30 LITERALlong    len_Nat value_Long
   *   | 31 LITERALfloat   len_Nat value_Long
   *   | 32 LITERALdouble  len_Nat value_Long
   *   | 33 LITERALstring  len_Nat  name_Ref
   *   | 34 LITERALnull    len_Nat
   *   | 35 LITERALclass   len_Nat   tpe_Ref
   *   | 36 LITERALenum    len_Nat   sym_Ref
   *   | 37 LITERALsymbol  len_Nat  name_Ref
   *   |
   *   | 40 SYMANNOT         len_Nat            sym_Ref  AnnotInfoBody
   *   | 41 CHILDREN         len_Nat            sym_Ref     {sym_Ref}
   *   | 42     ANNOTATEDtpe len_Nat           [sym_Ref]     tpe_Ref {annotinfo_Ref} // sym_Ref no longer needed
   *   | 43 ANNOTINFO        len_Nat      AnnotInfoBody
   *   | 44 ANNOTARGARRAY    len_Nat {constAnnotArg_Ref}
   *   | 47 DEBRUIJNINDEXtpe len_Nat          level_Nat    index_Nat   // deprecated
   *   | 48   EXISTENTIALtpe len_Nat           type_Ref  {symbol_Ref}
   */

  /**  | 49 TREE len_Nat
   *   |    1           EMPTYtree
   *   |    2         PACKAGEtree type_Ref      sym_Ref        mods_Ref  name_Ref      {tree_Ref}
   *   |    3           CLASStree type_Ref      sym_Ref        mods_Ref  name_Ref       tree_Ref {tree_Ref}
   *   |    4          MODULEtree type_Ref      sym_Ref        mods_Ref  name_Ref       tree_Ref
   *   |    5          VALDEFtree type_Ref      sym_Ref        mods_Ref  name_Ref       tree_Ref  tree_Ref
   *   |    6          DEFDEFtree type_Ref      sym_Ref        mods_Ref  name_Ref numtparams_Nat {tree_Ref} numparamss_Nat {numparams_Nat {tree_Ref}} tree_Ref tree_Ref
   *   |    7         TYPEDEFtree type_Ref      sym_Ref        mods_Ref  name_Ref       tree_Ref {tree_Ref}
   *   |    8           LABELtree type_Ref      sym_Ref        tree_Ref {tree_Ref}
   *   |    9          IMPORTtree type_Ref      sym_Ref        tree_Ref {name_Ref       name_Ref}
   *   |   11          DOCDEFtree type_Ref      sym_Ref      string_Ref  tree_Ref
   *   |   12        TEMPLATEtree type_Ref      sym_Ref  numparents_Nat {tree_Ref}      tree_Ref {tree_Ref}
   *   |   13           BLOCKtree type_Ref     tree_Ref       {tree_Ref}
   *   |   14            CASEtree type_Ref     tree_Ref        tree_Ref  tree_Ref
   *   |   15        SEQUENCEtree type_Ref    {tree_Ref}
   *   |   16     ALTERNATIVEtree type_Ref    {tree_Ref}
   *   |   17            STARtree type_Ref    {tree_Ref}
   *   |   18            BINDtree type_Ref      sym_Ref  name_Ref  tree_Ref
   *   |   19         UNAPPLYtree type_Ref     tree_Ref {tree_Ref}
   *   |   20      ARRAYVALUEtree type_Ref     tree_Ref {tree_Ref}
   *   |   21        FUNCTIONtree type_Ref      sym_Ref  tree_Ref {tree_Ref}
   *   |   22          ASSIGNtree type_Ref     tree_Ref  tree_Ref
   *   |   23              IFtree type_Ref     tree_Ref  tree_Ref  tree_Ref
   *   |   24           MATCHtree type_Ref     tree_Ref {tree_Ref}
   *   |   25          RETURNtree type_Ref      sym_Ref  tree_Ref
   *   |   26             TREtree type_Ref     tree_Ref  tree_Ref {tree_Ref}
   *   |   27           THROWtree type_Ref     tree_Ref
   *   |   28             NEWtree type_Ref     tree_Ref
   *   |   29           TYPEDtree type_Ref     tree_Ref  tree_Ref
   *   |   30       TYPEAPPLYtree type_Ref     tree_Ref {tree_Ref}
   *   |   31           APPLYtree type_Ref     tree_Ref {tree_Ref}
   *   |   32    APPLYDYNAMICtree type_Ref      sym_Ref  tree_Ref {tree_Ref}
   *   |   33           SUPERtree type_Ref      sym_Ref  tree_Ref  name_Ref
   *   |   34            THIStree type_Ref      sym_Ref  name_Ref
   *   |   35          SELECTtree type_Ref      sym_Ref  tree_Ref  name_Ref
   *   |   36           IDENTtree type_Ref      sym_Ref  name_Ref
   *   |   37         LITERALtree type_Ref constant_Ref
   *   |   38            TYPEtree type_Ref
   *   |   39       ANNOTATEDtree type_Ref     tree_Ref  tree_Ref
   *   |   40   SINGLETONTYPEtree type_Ref     tree_Ref
   *   |   41  SELECTFROMTYPEtree type_Ref     tree_Ref  name_Ref
   *   |   42    COMPOUNDTYPEtree type_Ref     tree_Ref
   *   |   43     APPLIEDTYPEtree type_Ref     tree_Ref {tree_Ref}
   *   |   44      TYPEBOUNDStree type_Ref     tree_Ref  tree_Ref
   *   |   45 EXISTENTIALTYPEtree type_Ref     tree_Ref {tree_Ref}
   *   |
   *   | 50 MODIFIERS len_Nat flags_Long privateWithin_Ref
   */

  val MajorVersion = 5
  val MinorVersion = 2

  // name
  final val TERMname          = 1
  final val TYPEname          = 2

  // sym
  final val   NONEsym         = 3
  final val   TYPEsym         = 4
  final val  ALIASsym         = 5
  final val  CLASSsym         = 6
  final val MODULEsym         = 7
  final val    VALsym         = 8

  // ref
  final val         EXTref    = 9
  final val EXTMODCLASSref    = 10

  // tpe
  final val             NOtpe = 11
  final val       NOPREFIXtpe = 12
  final val           THIStpe = 13
  final val         SINGLEtpe = 14
  final val       CONSTANTtpe = 15
  final val        TYPEREFtpe = 16
  final val     TYPEBOUNDStpe = 17
  final val        REFINEDtpe = 18
  final val      CLASSINFOtpe = 19
  final val         METHODtpe = 20
  final val           POLYtpe = 21
  final val IMPLICITMETHODtpe = 22    // no longer generated

  // literal
  final val LITERAL           = 23   // base line for literals
  final val LITERALunit       = 24
  final val LITERALboolean    = 25
  final val LITERALbyte       = 26
  final val LITERALshort      = 27
  final val LITERALchar       = 28
  final val LITERALint        = 29
  final val LITERALlong       = 30
  final val LITERALfloat      = 31
  final val LITERALdouble     = 32
  final val LITERALstring     = 33
  final val LITERALnull       = 34
  final val LITERALclass      = 35
  final val LITERALenum       = 36
  final val LITERALsymbol     = 37 // TODO: Never pickled, to be dropped once we have a STARR that does not emit it.

  final val SYMANNOT          = 40
  final val CHILDREN          = 41
  final val ANNOTATEDtpe      = 42
  final val ANNOTINFO         = 43
  final val ANNOTARGARRAY     = 44
  final val SUPERtpe          = 46
  final val DEBRUIJNINDEXtpe  = 47   // no longer generated
  final val EXISTENTIALtpe    = 48

  final val TREE              = 49      // prefix code that means a tree is coming
    final val EMPTYtree           = 1
    final val PACKAGEtree         = 2
    final val CLASStree           = 3
    final val MODULEtree          = 4
    final val VALDEFtree          = 5
    final val DEFDEFtree          = 6
    final val TYPEDEFtree         = 7
    final val LABELtree           = 8
    final val IMPORTtree          = 9
    final val DOCDEFtree          = 11
    final val TEMPLATEtree        = 12
    final val BLOCKtree           = 13
    final val CASEtree            = 14
    final val SEQUENCEtree        = 15 // This node type has been removed.
    final val ALTERNATIVEtree     = 16
    final val STARtree            = 17
    final val BINDtree            = 18
    final val UNAPPLYtree         = 19
    final val ARRAYVALUEtree      = 20
    final val FUNCTIONtree        = 21
    final val ASSIGNtree          = 22
    final val IFtree              = 23
    final val MATCHtree           = 24
    final val RETURNtree          = 25
    final val TREtree             = 26
    final val THROWtree           = 27
    final val NEWtree             = 28
    final val TYPEDtree           = 29
    final val TYPEAPPLYtree       = 30
    final val APPLYtree           = 31
    final val APPLYDYNAMICtree    = 32
    final val SUPERtree           = 33
    final val THIStree            = 34
    final val SELECTtree          = 35
    final val IDENTtree           = 36
    final val LITERALtree         = 37
    final val TYPEtree            = 38
    final val ANNOTATEDtree       = 39
    final val SINGLETONTYPEtree   = 40
    final val SELECTFROMTYPEtree  = 41
    final val COMPOUNDTYPEtree    = 42
    final val APPLIEDTYPEtree     = 43
    final val TYPEBOUNDStree      = 44
    final val EXISTENTIALTYPEtree = 45

  final val MODIFIERS         = 50

  final val   firstSymTag = NONEsym
  final val    lastSymTag = VALsym
  final val lastExtSymTag = EXTMODCLASSref

  def tag2string(tag: Int): String = tag match {
    case TERMname          => "TERMname"
    case TYPEname          => "TYPEname"

    case   NONEsym         =>   "NONEsym"
    case   TYPEsym         =>   "TYPEsym"
    case  ALIASsym         =>  "ALIASsym"
    case  CLASSsym         =>  "CLASSsym"
    case MODULEsym         => "MODULEsym"
    case    VALsym         =>    "VALsym"

    case         EXTref    =>         "EXTref"
    case EXTMODCLASSref    => "EXTMODCLASSref"

    case             NOtpe =>             "NOtpe"
    case       NOPREFIXtpe =>       "NOPREFIXtpe"
    case           THIStpe =>           "THIStpe"
    case         SINGLEtpe =>         "SINGLEtpe"
    case       CONSTANTtpe =>       "CONSTANTtpe"
    case        TYPEREFtpe =>        "TYPEREFtpe"
    case     TYPEBOUNDStpe =>     "TYPEBOUNDStpe"
    case        REFINEDtpe =>        "REFINEDtpe"
    case      CLASSINFOtpe =>      "CLASSINFOtpe"
    case         METHODtpe =>         "METHODtpe"
    case           POLYtpe =>           "POLYtpe"
    case IMPLICITMETHODtpe => "IMPLICITMETHODtpe" // no longer used
    case          SUPERtpe =>          "SUPERtpe"

    case LITERALunit       => "LITERALunit"
    case LITERALboolean    => "LITERALboolean"
    case LITERALbyte       => "LITERALbyte"
    case LITERALshort      => "LITERALshort"
    case LITERALchar       => "LITERALchar"
    case LITERALint        => "LITERALint"
    case LITERALlong       => "LITERALlong"
    case LITERALfloat      => "LITERALfloat"
    case LITERALdouble     => "LITERALdouble"
    case LITERALstring     => "LITERALstring"
    case LITERALnull       => "LITERALnull"
    case LITERALclass      => "LITERALclass"
    case LITERALenum       => "LITERALenum"
    case LITERALsymbol     => "LITERALsymbol"

    case SYMANNOT          => "SYMANNOT"
    case CHILDREN          => "CHILDREN"
    case      ANNOTATEDtpe =>      "ANNOTATEDtpe"
    case ANNOTINFO         => "ANNOTINFO"
    case ANNOTARGARRAY     => "ANNOTARGARRAY"
    case    EXISTENTIALtpe =>    "EXISTENTIALtpe"

    case TREE              => "TREE"
    case MODIFIERS         => "MODIFIERS"
    case _                 => s"***BAD TAG***($tag)"
  }
}
