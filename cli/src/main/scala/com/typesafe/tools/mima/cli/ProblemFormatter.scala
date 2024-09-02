package com.typesafe.tools.mima.cli

import com.typesafe.tools.mima.core.AbstractMethodProblem
import com.typesafe.tools.mima.core.DirectMissingMethodProblem
import com.typesafe.tools.mima.core.FinalMethodProblem
import com.typesafe.tools.mima.core.InaccessibleFieldProblem
import com.typesafe.tools.mima.core.InaccessibleMethodProblem
import com.typesafe.tools.mima.core.IncompatibleFieldTypeProblem
import com.typesafe.tools.mima.core.IncompatibleMethTypeProblem
import com.typesafe.tools.mima.core.IncompatibleResultTypeProblem
import com.typesafe.tools.mima.core.IncompatibleSignatureProblem
import com.typesafe.tools.mima.core.MemberInfo
import com.typesafe.tools.mima.core.MemberProblem
import com.typesafe.tools.mima.core.MissingFieldProblem
import com.typesafe.tools.mima.core.MissingMethodProblem
import com.typesafe.tools.mima.core.NewMixinForwarderProblem
import com.typesafe.tools.mima.core.Problem
import com.typesafe.tools.mima.core.ReversedAbstractMethodProblem
import com.typesafe.tools.mima.core.ReversedMissingMethodProblem
import com.typesafe.tools.mima.core.TemplateProblem
import com.typesafe.tools.mima.core.UpdateForwarderBodyProblem

case class ProblemFormatter(
    showForward: Boolean = true,
    showBackward: Boolean = true,
    showIncompatibleSignature: Boolean = false,
    useBytecodeNames: Boolean = false,
    showDescriptions: Boolean = false
) {

  private def str(problem: TemplateProblem): String =
    s"${if (useBytecodeNames) problem.ref.bytecodeName
      else problem.ref.fullName}: ${problem.getClass.getSimpleName.stripSuffix("Problem")}${description(problem)}"

  private def str(problem: MemberProblem): String =
    s"${memberName(problem.ref)}: ${problem.getClass.getSimpleName.stripSuffix("Problem")}${description(problem)}"

  private def description(problem: Problem): String =
    if (showDescriptions) ": " + problem.description("new") else ""

  private def memberName(info: MemberInfo): String =
    if (useBytecodeNames)
      bytecodeFullName(info)
    else
      info.fullName

  private def bytecodeFullName(info: MemberInfo): String = {
    val pkg = info.owner.owner.fullName.replace('.', '/')
    val clsName = info.owner.bytecodeName
    val memberName = info.bytecodeName match {
      case "<init>" => "\"<init>\""
      case name     => name
    }
    val sig = info.descriptor

    s"$pkg/$clsName.$memberName$sig"
  }

  // format: off
  def formatProblem(problem: Problem): Option[String] = problem match {
    case prob: TemplateProblem if showBackward  => Some(str(prob))
    case _:    TemplateProblem                  => None

    case problem: MemberProblem => problem match {
      case prob: AbstractMethodProblem if showBackward  => Some(str(prob))
      case _:    AbstractMethodProblem                  => None

      case problem: MissingMethodProblem => problem match {
        case prob: DirectMissingMethodProblem if showBackward   => Some(str(prob))
        case _:    DirectMissingMethodProblem                   => None
        case prob: ReversedMissingMethodProblem if showForward  => Some(str(prob))
        case _:    ReversedMissingMethodProblem                 => None
      }

      case prob: ReversedAbstractMethodProblem if showForward   => Some(str(prob))
      case _:    ReversedAbstractMethodProblem                  => None
      case prob: MissingFieldProblem if showBackward            => Some(str(prob))
      case _:    MissingFieldProblem                            => None
      case prob: InaccessibleFieldProblem if showBackward       => Some(str(prob))
      case _:    InaccessibleFieldProblem                       => None
      case prob: IncompatibleFieldTypeProblem if showBackward   => Some(str(prob))
      case _:    IncompatibleFieldTypeProblem                   => None
      case prob: InaccessibleMethodProblem if showBackward      => Some(str(prob))
      case _:    InaccessibleMethodProblem                      => None
      case prob: IncompatibleMethTypeProblem if showBackward    => Some(str(prob))
      case _:    IncompatibleMethTypeProblem                    => None
      case prob: IncompatibleResultTypeProblem if showBackward  => Some(str(prob))
      case _:    IncompatibleResultTypeProblem                  => None
      case prob: FinalMethodProblem if showBackward             => Some(str(prob))
      case _:    FinalMethodProblem                             => None
      case prob: UpdateForwarderBodyProblem if showBackward     => Some(str(prob))
      case _:    UpdateForwarderBodyProblem                     => None
      case prob: NewMixinForwarderProblem if showBackward       => Some(str(prob))
      case _:    NewMixinForwarderProblem                       => None

      case prob: IncompatibleSignatureProblem
        if showBackward && showIncompatibleSignature => Some(str(prob))
      case _:    IncompatibleSignatureProblem        => None
    }
  }
  // format: on

}
