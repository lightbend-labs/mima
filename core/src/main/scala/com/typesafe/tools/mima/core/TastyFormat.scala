package com.typesafe.tools.mima.core

/** BNF notation.
 *  Terminal symbols start with 2 upper case letters, and are represented as a single byte tag.
 *  Non-terminals are mixed case.
 *  Lower case letter prefixes, followed by an underscore, are only descriptive.
 *
 *  Micro-syntax:
 *       Digit =   0 | ... | 127
 *   StopDigit = 128 | ... | 255         -- value = digit - 128
 *   LongInt   = Digit* StopDigit        -- big endian 2's complement, value fits in a Long w/o overflow
 *   Int       = LongInt                 -- big endian 2's complement, fits in an Int w/o overflow
 *   Nat       = LongInt                 -- non-negative value, fits in an Int without overflow
 *   Length    = Nat                     -- length of rest of entry in bytes
 *
 *  Macro-format:
 *   File           = Header FormatVersion ToolingVersion UUID nameTable_Length Name* Section*
 *   Header         = 0x5C 0xA1 0xAB 0x1F
 *   FormatVersion  = majorVersion_Nat minorVersion_Nat experimentalVersion_Nat
 *   ToolingVersion = Length UTF8-CodePoint*  -- string that represents the tool that produced the TASTy
 *   UUID           = Byte*16                 -- random UUID
 *   Section        = NameRef Length Bytes */
object TastyFormat {
  val header              = Array[Int](0x5C, 0xA1, 0xAB, 0x1F)
  val MajorVersion        = 28
  val MinorVersion        = 0
  val ExperimentalVersion = 0

  /** Name table:
   *   Name           = UTF8              Length UTF8-CodePoint*
   *                    QUALIFIED         Length qualified_NameRef selector_NameRef               -- A.B
   *                    EXPANDED          Length qualified_NameRef selector_NameRef               -- A$$B, semantically a NameKinds.ExpandedName
   *                    EXPANDPREFIX      Length qualified_NameRef selector_NameRef               -- A$B, prefix of expanded name, see NamedKinds.ExpandPrefixName
   *
   *                    UNIQUE            Length separator_NameRef uniqid_Nat underlying_NameRef? -- Unique name A<separator><number>
   *                    DEFAULTGETTER     Length underlying_NameRef index_Nat                     -- DefaultGetter$<number>
   *
   *                    SUPERACCESSOR     Length underlying_NameRef                               -- super$A
   *                    INLINEACCESSOR    Length underlying_NameRef                               -- inline$A
   *                    OBJECTCLASS       Length underlying_NameRef                               -- A$  (name of the module class for module A)
   *
   *                    SIGNED            Length original_NameRef resultSig_NameRef ParamSig*     -- name + signature
   *                    TARGETSIGNED      Length original_NameRef target_NameRef resultSig_NameRef ParamSig*
   *
   *   ParamSig       = Int -- If negative, the absolute value represents the length of a type parameter section
   *                        -- If positive, this is a NameRef for the fully qualified name of a term parameter.
   *
   *   NameRef        = Nat -- ordinal number of name in name table, starting from 1.
   *
   * Note: Unqualified names in the name table are strings. The context decides whether a name is
   * a type-name or a term-name. The same string can represent both.
   */
  object NameTags {
    val UTF8           = 1  // A simple name in UTF8 encoding.
    val QUALIFIED      = 2  // A fully qualified name `<prefix>.<suffix>`.
    val EXPANDED       = 3  // An expanded name `<prefix>$$<suffix>`, used by Scala 2 for private names.
    val EXPANDPREFIX   = 4  // An expansion prefix `<prefix>$<suffix>`, used by Scala 2 for private names.
    val UNIQUE         = 10 // A unique name `<name>$<num>` where `<num>` is used only once for each `<name>`.
    val DEFAULTGETTER  = 11 // The name `<meth-name>$default$<param-num>` of a default getter that returns a default argument.
    val SUPERACCESSOR  = 20 // The name of a super accessor `super$name` created by SuperAccesors.
    val INLINEACCESSOR = 21 // The name of an inline accessor `inline$name`
    val BODYRETAINER   = 22 // The name of a synthetic method that retains the runtime body of an inline method
    val OBJECTCLASS    = 23 // The name of an object class (or: module class) `<name>$`.
    val SIGNED         = 63 // A pair of a name and a signature, used to identify possibly overloaded methods.
    val TARGETSIGNED   = 62 // A triple of a name, a targetname and a signature, used to identify possibly overloaded methods that carry a @targetName annotation.
  }

  /** Standard-Section: "ASTs" Tree*
   *   Tree          = PACKAGE        Length Path Tree*                                 -- package path { topLevelStats }
   *                   Stat
   *
   *   Stat          = Term
   *                   ValOrDefDef
   *                   TYPEDEF        Length NameRef (type_Term | Template) Modifier*   -- modifiers type name (= type | bounds)  |  modifiers class name template
   *                   IMPORT         Length qual_Term Selector*                        -- import qual selectors
   *                   EXPORT         Length qual_Term Selector*                        -- export qual selectors
   *   ValOrDefDef   = VALDEF         Length NameRef              type_Term rhs_Term? Modifier*  -- modifiers val name : type (= rhs)?
   *                   DEFDEF         Length NameRef Param* returnType_Term rhs_Term? Modifier*  -- modifiers def name [typeparams] paramss : returnType (= rhs)?
   *   Selector      = IMPORTED              NameRef                                    -- name, "_" for normal wildcards, "" for given wildcards
   *                   RENAMED               to_NameRef                                 -- => name
   *                   BOUNDED               type_Term                                  -- type bound
   *
   *   TypeParam     = TYPEPARAM      Length NameRef type_Term Modifier*                -- modifiers name bounds
   *   TermParam     = PARAM          Length NameRef type_Term Modifier*                -- modifiers name : type.
   *                   EMPTYCLAUSE                                                      -- an empty parameter clause ()
   *                   SPLITCLAUSE                                                      -- splits two non-empty parameter clauses of the same kind
   *   Param         = TypeParam
   *                   TermParam
   *   Template      = TEMPLATE       Length TypeParam* TermParam* parent_Term* Self? Stat*  -- [typeparams] paramss extends parents { self => stats }, where Stat* always starts with the primary constructor.
   *   Self          = SELFDEF               selfName_NameRef selfType_Term             -- selfName : selfType
   *
   *   Term          = Path                                                             -- Paths represent both types and terms
   *                   IDENT                 NameRef Type                               -- Used when term ident’s type is not a TermRef
   *                   SELECT                possiblySigned_NameRef qual_Term           -- qual.name
   *                   SELECTin       Length possiblySigned_NameRef qual_Term owner_Type -- qual.name, referring to a symbol declared in owner that has the given signature (see note below)
   *                   QUALTHIS              typeIdent_Tree                             -- id.this, different from THIS in that it contains a qualifier ident with position.
   *                   NEW                   clsType_Term                               -- new cls
   *                   THROW                 throwableExpr_Term                         -- throw throwableExpr
   *                   NAMEDARG              paramName_NameRef arg_Term                 -- paramName = arg
   *                   APPLY          Length fn_Term arg_Term*                          -- fn(args)
   *                   TYPEAPPLY      Length fn_Term arg_Type*                          -- fn[args]
   *                   SUPER          Length this_Term mixinTypeIdent_Tree?             -- super[mixin]
   *                   TYPED          Length expr_Term ascriptionType_Term              -- expr: ascription
   *                   ASSIGN         Length lhs_Term rhs_Term                          -- lhs = rhs
   *                   BLOCK          Length expr_Term Stat*                            -- { stats; expr }
   *                   INLINED        Length expr_Term call_Term? ValOrDefDef*          -- Inlined code from call, with given body `expr` and given bindings
   *                   LAMBDA         Length meth_Term target_Type?                     -- Closure over method `f` of type `target` (omitted id `target` is a function type)
   *                   IF             Length [INLINE] cond_Term then_Term else_Term     -- inline? if cond then thenPart else elsePart
   *                   MATCH          Length (IMPLICIT | [INLINE] sel_Term) CaseDef*    -- (inline? sel | implicit) match caseDefs
   *                   TRY            Length expr_Term CaseDef* finalizer_Term?         -- try expr catch {casdeDef} (finally finalizer)?
   *                   RETURN         Length meth_ASTRef expr_Term?                     -- return expr?,  `methASTRef` is method from which is returned
   *                   WHILE          Length cond_Term body_Term                        -- while cond do body
   *                   REPEATED       Length elem_Type elem_Term*                       -- Varargs argument of type `elem`
   *                   SELECTouter    Length levels_Nat qual_Term underlying_Type       -- Follow `levels` outer links, starting from `qual`, with given `underlying` type
   *     -- patterns:
   *                   BIND           Length boundName_NameRef patType_Type pat_Term    -- name @ pat, wherev `patType` is the type of the bound symbol
   *                   ALTERNATIVE    Length alt_Term*                                  -- alt1 | ... | altn   as a pattern
   *                   UNAPPLY        Length fun_Term ImplicitArg* pat_Type pat_Term*   -- Unapply node `fun(_: pat_Type)(implicitArgs)` flowing into patterns `pat`.
   *     -- type trees:
   *                        IDENTtpt         NameRef Type                               -- Used for all type idents
   *                       SELECTtpt         NameRef qual_Term                          -- qual.name
   *                    SINGLETONtpt         ref_Term                                   -- ref.type
   *                      REFINEDtpt  Length underlying_Term refinement_Stat*           -- underlying {refinements}
   *                      APPLIEDtpt  Length tycon_Term arg_Term*                       -- tycon [args]
   *                       LAMBDAtpt  Length TypeParam* body_Term                       -- [TypeParams] => body
   *                   TYPEBOUNDStpt  Length low_Term high_Term?                        -- {{{ >: low <: high }}}
   *                    ANNOTATEDtpt  Length underlying_Term fullAnnotation_Term        -- underlying @ annotation
   *                        MATCHtpt  Length bound_Term? sel_Term CaseDef*              -- sel match { CaseDef } where `bound` is optional upper bound of all rhs
   *                       BYNAMEtpt         underlying_Term                            -- => underlying
   *                   SHAREDterm            term_ASTRef                                -- Link to previously serialized term
   *                   HOLE           Length idx_Nat arg_Tree*                          -- Hole where a splice goes with sequence number idx, splice is applied to arguments `arg`s
   *
   *   CaseDef       = CASEDEF        Length pat_Term rhs_Tree guard_Tree?              -- case pat if guard => rhs
   *   ImplicitArg   = IMPLICITARG           arg_Term                                   -- implicit unapply argument
   *
   *   ASTRef        = Nat                                                              -- Byte position in AST payload
   *
   *   Path          = Constant
   *                   TERMREFdirect         sym_ASTRef                                 -- A reference to a local symbol (without a prefix). Reference is to definition node of symbol.
   *                   TERMREFsymbol         sym_ASTRef qual_Type                       -- A reference `qual.sym` to a local member with prefix `qual`
   *                   TERMREFpkg            fullyQualified_NameRef                     -- A reference to a package member with given fully qualified name
   *                   TERMREF               possiblySigned_NameRef qual_Type           -- A reference `qual.name` to a non-local member
   *                   TERMREFin      Length possiblySigned_NameRef qual_Type owner_Type -- A reference `qual.name` referring to a non-local symbol declared in owner that has the given signature (see note below)
   *                   THIS                  clsRef_Type                                -- cls.this
   *                   RECthis               recType_ASTRef                             -- The `this` in a recursive refined type `recType`.
   *                   SHAREDtype            path_ASTRef                                -- link to previously serialized path
   *
   *   Constant      =   UNITconst                                                      -- ()
   *                    FALSEconst                                                      -- false
   *                     TRUEconst                                                      -- true
   *                     BYTEconst           Int                                        -- A byte number
   *                    SHORTconst           Int                                        -- A short number
   *                     CHARconst           Nat                                        -- A character
   *                      INTconst           Int                                        -- An int number
   *                     LONGconst           LongInt                                    -- A long number
   *                    FLOATconst           Int                                        -- A float number
   *                   DOUBLEconst           LongInt                                    -- A double number
   *                   STRINGconst           NameRef                                    -- A string literal
   *                     NULLconst                                                      -- null
   *                    CLASSconst           Type                                       -- classOf[Type]
   *
   *   Type          = Path                                                             -- Paths represent both types and terms
   *                   TYPEREFdirect         sym_ASTRef                                 -- A reference to a local symbol (without a prefix). Reference is to definition node of symbol.
   *                   TYPEREFsymbol         sym_ASTRef qual_Type                       -- A reference `qual.sym` to a local member with prefix `qual`
   *                   TYPEREFpkg            fullyQualified_NameRef                     -- A reference to a package member with given fully qualified name
   *                   TYPEREF               NameRef qual_Type                          -- A reference `qual.name` to a non-local member
   *                   TYPEREFin      Length NameRef qual_Type namespace_Type           -- A reference `qual.name` to a non-local member that's private in `namespace`.
   *                         RECtype         parent_Type                                -- A wrapper for recursive refined types
   *                       SUPERtype  Length this_Type underlying_Type                  -- A super type reference to `underlying`
   *                     REFINEDtype  Length underlying_Type refinement_NameRef info_Type -- underlying { refinement_name : info }
   *                     APPLIEDtype  Length tycon_Type arg_Type*                       -- tycon[args]
   *                   TYPEBOUNDS     Length lowOrAlias_Type high_Type? Variance*       -- = alias or {{{ >: low <: high }}}, possibly with variances of lambda parameters
   *                    ANNOTATEDtype Length underlying_Type annotation_Term            -- underlying @ annotation
   *                          ANDtype Length left_Type right_Type                       -- left & right
   *                           ORtype Length left_Type right_Type                       -- lefgt | right
   *                   MATCHtype      Length bound_Type sel_Type case_Type*             -- sel match {cases} with optional upper `bound`
   *                   MATCHCASEtype  Length pat_type rhs_Type                          -- match cases are MATCHCASEtypes or TYPELAMBDAtypes over MATCHCASEtypes
   *                   BIND           Length boundName_NameRef bounds_Type Modifier*    -- boundName @ bounds,  for type-variables defined in a type pattern
   *                       BYNAMEtype        underlying_Type                            -- => underlying
   *                        PARAMtype Length binder_ASTRef paramNum_Nat                 -- A reference to parameter # paramNum in lambda type `binder`
   *                         POLYtype Length result_Type TypesNames                     -- A polymorphic method type `[TypesNames]result`, used in refinements
   *                       METHODtype Length result_Type TypesNames Modifier*           -- A method type `(Modifier* TypesNames)result`, needed for refinements, with optional modifiers for the parameters
   *                   TYPELAMBDAtype Length result_Type TypesNames                     -- A type lambda `[TypesNames] => result`
   *                       SHAREDtype        type_ASTRef                                -- link to previously serialized type
   *   TypesNames    = TypeName*
   *   TypeName      = typeOrBounds_ASTRef paramName_NameRef                            -- (`termName`: `type`)  or  (`typeName` `bounds`)
   *
   *   Modifier      = PRIVATE                                                          -- private
   *                   PROTECTED                                                        -- protected
   *                     PRIVATEqualified   qualifier_Type                              --   private[qualifier]  (to be dropped(?)
   *                   PROTECTEDqualified   qualifier_Type                              -- protected[qualifier]  (to be dropped(?)
   *                   ABSTRACT                                                         -- abstract
   *                   FINAL                                                            -- final
   *                   SEALED                                                           -- sealed
   *                   CASE                                                             -- case  (for classes or objects)
   *                   IMPLICIT                                                         -- implicit
   *                   GIVEN                                                            -- given
   *                   ERASED                                                           -- erased
   *                   LAZY                                                             -- lazy
   *                   OVERRIDE                                                         -- override
   *                   OPAQUE                                                           -- opaque, also used for classes containing opaque aliases
   *                   INLINE                                                           -- inline
   *                   MACRO                                                            -- Inline method containing toplevel splices
   *                   INLINEPROXY                                                      -- Symbol of binding with an argument to an inline method as rhs (TODO: do we still need this?)
   *                   STATIC                                                           -- Mapped to static Java member
   *                   OBJECT                                                           -- An object or its class
   *                   TRAIT                                                            -- A trait
   *                   ENUM                                                             -- A enum class or enum case
   *                   LOCAL                                                            -- private[this] or protected[this], used in conjunction with PRIVATE or PROTECTED
   *                   SYNTHETIC                                                        -- Generated by Scala compiler
   *                   ARTIFACT                                                         -- To be tagged Java Synthetic
   *                   MUTABLE                                                          -- A var
   *                   FIELDaccessor                                                    -- A getter or setter (note: the corresponding field is not serialized)
   *                    CASEaccessor                                                    -- A getter for a case class parameter
   *                   COVARIANT                                                        -- A type parameter marked “+”
   *                   CONTRAVARIANT                                                    -- A type parameter marked “-”
   *                   HASDEFAULT                                                       -- Parameter with default arg; method with default parameters (default arguments are separate methods with DEFAULTGETTER names)
   *                   STABLE                                                           -- Method that is assumed to be stable, i.e. its applications are legal paths
   *                   EXTENSION                                                        -- An extension method
   *                   PARAMsetter                                                      -- The setter part `x_=` of a var parameter `x` which itself is pickled as a PARAM
   *                   PARAMalias                                                       -- Parameter is alias of a superclass parameter
   *                   EXPORTED                                                         -- An export forwarder
   *                   OPEN                                                             -- an open class
   *                   INVISIBLE                                                        -- invisible during typechecking
   *                   Annotation
   *
   *   Variance      = STABLE                                                           -- invariant
   *                 | COVARIANT
   *                 | CONTRAVARIANT
   *
   *   Annotation    = ANNOTATION     Length tycon_Type fullAnnotation_Term             -- An annotation, given (class) type of constructor, and full application tree
   *
   * Note: The signature of a SELECTin or TERMREFin node is the signature of the selected symbol,
   *       not the signature of the reference. The latter undergoes an asSeenFrom but the former
   *       does not.
   *
   * Note: Tree tags are grouped into 5 categories that determine what follows,
   *       and thus allow to compute the size of the tagged tree in a generic way.
   */
  val ASTsSection = "ASTs"

  val AstCat1       = 1 to  59       // Cat. 1: tag
  val  UNITconst    =  2
  val FALSEconst    =  3
  val  TRUEconst    =  4
  val  NULLconst    =  5
  val PRIVATE       =  6
  val PROTECTED     =  8
  val ABSTRACT      =  9
  val FINAL         = 10
  val SEALED        = 11
  val CASE          = 12
  val IMPLICIT      = 13
  val LAZY          = 14
  val OVERRIDE      = 15
  val INLINEPROXY   = 16
  val INLINE        = 17
  val STATIC        = 18
  val OBJECT        = 19
  val TRAIT         = 20
  val ENUM          = 21
  val LOCAL         = 22
  val SYNTHETIC     = 23
  val ARTIFACT      = 24
  val MUTABLE       = 25
  val FIELDaccessor = 26
  val  CASEaccessor = 27
  val COVARIANT     = 28
  val CONTRAVARIANT = 29
  val HASDEFAULT    = 31
  val STABLE        = 32
  val MACRO         = 33
  val ERASED        = 34
  val OPAQUE        = 35
  val EXTENSION     = 36
  val GIVEN         = 37
  val PARAMsetter   = 38
  val EXPORTED      = 39
  val OPEN          = 40
  val PARAMalias    = 41
  val TRANSPARENT   = 42
  val INFIX         = 43
  val INVISIBLE     = 44
  val EMPTYCLAUSE   = 45
  val SPLITCLAUSE   = 46

  val AstCat2       = 60 to  89      // Cat. 2: tag Nat
  val SHAREDterm    = 60
  val SHAREDtype    = 61
  val TERMREFdirect = 62
  val TYPEREFdirect = 63
  val TERMREFpkg    = 64
  val TYPEREFpkg    = 65
  val RECthis       = 66
  val   BYTEconst   = 67
  val  SHORTconst   = 68
  val   CHARconst   = 69
  val    INTconst   = 70
  val   LONGconst   = 71
  val  FLOATconst   = 72
  val DOUBLEconst   = 73
  val STRINGconst   = 74
  val IMPORTED      = 75
  val RENAMED       = 76

  val AstCat3            = 90 to 109 // Cat. 3: tag AST
  val THIS               = 90
  val QUALTHIS           = 91
  val CLASSconst         = 92
  val BYNAMEtype         = 93
  val BYNAMEtpt          = 94
  val NEW                = 95
  val THROW              = 96
  val IMPLICITarg        = 97
  val   PRIVATEqualified = 98
  val PROTECTEDqualified = 99
  val RECtype            = 100
  val SINGLETONtpt       = 101
  val BOUNDED            = 102

  val AstCat4       = 100 to 127     // Cat. 4: tag Nat AST
  val IDENT         = 110
  val IDENTtpt      = 111
  val SELECT        = 112
  val SELECTtpt     = 113
  val TERMREFsymbol = 114
  val TERMREF       = 115
  val TYPEREFsymbol = 116
  val TYPEREF       = 117
  val SELFDEF       = 118
  val NAMEDARG      = 119

  val AstCat5        = 128 to 255    // Cat. 5: tag Length [payload]
  val PACKAGE        = 128
  val VALDEF         = 129
  val DEFDEF         = 130
  val TYPEDEF        = 131
  val IMPORT         = 132
  val TYPEPARAM      = 133
  val PARAM          = 134
  val APPLY          = 136
  val TYPEAPPLY      = 137
  val TYPED          = 138
  val ASSIGN         = 139
  val BLOCK          = 140
  val IF             = 141
  val LAMBDA         = 142
  val MATCH          = 143
  val RETURN         = 144
  val WHILE          = 145
  val TRY            = 146
  val INLINED        = 147
  val SELECTouter    = 148
  val REPEATED       = 149
  val BIND           = 150
  val ALTERNATIVE    = 151
  val UNAPPLY        = 152
  val  ANNOTATEDtype = 153
  val  ANNOTATEDtpt  = 154
  val CASEDEF        = 155
  val TEMPLATE       = 156
  val SUPER          = 157
  val SUPERtype      = 158
  val REFINEDtype    = 159
  val REFINEDtpt     = 160
  val APPLIEDtype    = 161
  val APPLIEDtpt     = 162
  val TYPEBOUNDS     = 163
  val TYPEBOUNDStpt  = 164
  val        ANDtype = 165
  val         ORtype = 167
  val       POLYtype = 169
  val TYPELAMBDAtype = 170
  val     LAMBDAtpt  = 171
  val      PARAMtype = 172
  val ANNOTATION     = 173
  val TERMREFin      = 174
  val TYPEREFin      = 175
  val  SELECTin      = 176
  val EXPORT         = 177
  val METHODtype     = 180
  val MATCHtype      = 190
  val MATCHtpt       = 191
  val MATCHCASEtype  = 192
  val HOLE           = 255

  /** Standard-Section: "Positions" LinesSizes Assoc*
   *
   *   LinesSizes    = Nat Nat*                 // Number of lines followed by the size of each line not counting the trailing `\n`
   *
   *   Assoc         = Header offset_Delta? offset_Delta? point_Delta?
   *                 | SOURCE nameref_Int
   *   Header        = addr_Delta +              // in one Nat: difference of address to last recorded node << 3 +
   *                   hasStartDiff +            // one bit indicating whether there follows a start address delta << 2
   *                   hasEndDiff +              // one bit indicating whether there follows an end address delta << 1
   *                   hasPoint                  // one bit indicating whether the new position has a point (i.e ^ position)
   *                                             // Nodes which have the same positions as their parents are omitted.
   *                                             // offset_Deltas give difference of start/end offset wrt to the
   *                                             // same offset in the previously recorded node (or 0 for the first recorded node)
   *   Delta         = Int                       // Difference between consecutive offsets,
   *   SOURCE        = 4                         // Impossible as header, since addr_Delta = 0 implies that we refer to the
   *                                             // same tree as the previous one, but then hasStartDiff = 1 implies that
   *                                             // the tree's range starts later than the range of itself.
   *
   * All elements of a position section are serialized as Ints
   */
  val PositionsSection = "Positions"
  val SOURCE = 4 // Position header

  /** Standard Section: "Comments" Comment*
   *   Comment       = Length Bytes LongInt      // Raw comment's bytes encoded as UTF-8, followed by the comment's coordinates.
   */
  val CommentsSection = "Comments"
}
