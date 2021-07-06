/*
 *
 * Copyright (C) 2021 University of Bamberg, Software Technologies Research Group
 * <https://www.uni-bamberg.de/>, <http://www.swt-bamberg.de/>
 *
 * This file is part of the Foundations of Heterogeneous Specifications Using
 * State Machines and Temporal Logic (IA-Toolset) project, which received financial
 * support by the German Research Foundation (DFG) under grant nos. LU 1748/3-2,
 * VO 615/12-2, see <https://www.swt-bamberg.de/research/interface-theories.html>.
 *
 * IA-Toolset is licensed under the GNU GENERAL PUBLIC LICENSE (Version 3), see
 * the LICENSE file at the project's top-level directory for details or consult
 * <http://www.gnu.org/licenses/>.
 *
 * IA-Toolset is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 *
 * IA-Toolset is a RESEARCH PROTOTYPE and distributed WITHOUT ANY
 * WARRANTY, without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * The following people contributed to the conception and realization of the
 * present IA-Toolset distribution (in alphabetic order by surname):
 *
 * - Tri Nguyen (https://github.com/trinnguyen)
 *
 */

package swtia.validation

import swtia.ia.GActionSuffix
import swtia.transformation.iam.IamValidator
import swtia.validation.typing.DataType

object ErrorMessages {

    fun expectSameType(leftType: DataType, rightType: DataType): String {
        return "expected same types but actual: '$leftType' and '$rightType'"
    }

    fun expectType(expectedType: DataType, actual: DataType): String {
        return expectType(expectedType.toString(), actual.toString())
    }

    fun expectType(expectedType: String, actual: String): String {
        return "expected type '$expectedType' but actual '$actual'"
    }

    fun expectBoolOrAction(actual: String): String {
        return "expected type 'bool' but actual '$actual'"
    }

    fun cannotJumpOutsideLoop(name: String): String {
        return "cannot '$name' outside of a loop"
    }

    fun idIsAlreadyDefined(name: String): String {
        return "identifier '$name' is already defined"
    }

    fun localVariableIsNotSupported(name: String): String {
        return "local variable is not supported, move '$name' to global"
    }

    fun procIsAlreadyDefined(name: String): String {
        return "process '$name' is already defined"
    }

    fun actionIsAlreadyDefined(name: String): String {
        return "action '$name' is already defined"
    }

    fun customTypeIsAlreadyDefined(name: String): String {
        return "custom type '$name' is already defined"
    }

    fun actionIsAlreadyDeclaredInProc(name: String): String {
        return "action '$name' is already declared in proc"
    }

    fun sysIsAlreadyDefined(name: String): String {
        return "system '$name' is already defined"
    }

    fun invalidActionSuffix(name: String, expect: GActionSuffix, actual: GActionSuffix): String {
        return "invalid action suffix, expect '$name$expect' but actual '$name$actual'"
    }

    fun noActionFoundForPostCondition(): String {
        return "no action found for post condition"
    }

    fun invalidPostCondition(expectedAssert: String, strtAction: String): String {
        return "invalid post condition, expected $expectedAssert for $strtAction"
    }

    fun errorProcCallMismatchedArgs(expected: Int, actual: Int): String {
        return "number of arguments are not matched, expected $expected but actual $actual"
    }

    fun errorDataDeterministic(fmtAction: String, strSteps: String): String {
        return "Data-deterministic is required for input $fmtAction: $strSteps"
    }

    fun errorIamPreCondNotSat(step: String): String {
        return "pre-cond is not satisfiable at $step"
    }

    fun errorIamPostCondNotSat(step: String): String {
        return "post-cond is not satisfiable at $step"
    }

    fun errorUnsatisfiablePostCond(formatStep: String): String {
        return "Unsatisfiable post-condition with primed variable at $formatStep"
    }

    fun incompatibleIas(s1: String, s2: String): String {
        return "The $s1 and $s2 are incompatible"
    }

    fun missingTools(tools: List<String>): String {
        return "Missing tools in PATH env: ${tools.joinToString(", ")}"
    }

    const val errorPrimeIsOnlyAllowedInAssert = "primed variable is only allowed in 'assume' or 'guarantee'"

    const val errorPrimeIsForGlobalVarOnly = "primed reference is for global variable only"

    const val actionNotAllowedInExpr = "action synchronization cannot be used with other expressions"

    const val elseBranchNotAllowedWithAction = "else branch is not allowed with action on condition"

    const val cannotComputeType = "type cannot be computed"

    const val sharedVariableIsNotAllowed = "shared variable is not allowed in process creation"

    const val disjunctiveMustStatementNotAllowed = "disjunctive statement is not allowed in IAM (use #mia instead)"

    const val mayActionNotAllowed = "may action is not allowed in IAM (use #mia instead)"

    const val dataTypesNotAllowed = "custom data type is not allowed in MIA (use #iam instead)"

    const val expressionNotAllowed = "expression is not allowed in MIA (use #iam instead)"

    const val variableNotAllowedInMia = "variable is not allowed in MIA (use #iam instead)"

    const val disjunctiveStatementMustFollowMust = "disjunctive statement must follow a must action"

    const val initStateIsPruned = "invalid result automaton of product (init state is pruned)"
}