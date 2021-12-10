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
        return "Expected same types but got '$leftType' and '$rightType'"
    }

    fun expectType(expectedType: DataType, actual: DataType): String {
        return expectType(expectedType.toString(), actual.toString())
    }

    fun expectType(expectedType: String, actual: String): String {
        return "Expected type '$expectedType' but got '$actual'"
    }

    fun expectBoolOrAction(actual: String): String {
        return "Expected type 'bool' but got '$actual'"
    }

    fun cannotJumpOutsideLoop(name: String): String {
        return "Cannot '$name' outside of a loop"
    }

    fun idIsAlreadyDefined(name: String): String {
        return "Redefinition of identifier '$name'"
    }

    fun localVariableIsNotSupported(name: String): String {
        return "Local variables are not supported: move '$name' to global scope"
    }

    fun procIsAlreadyDefined(name: String): String {
        return "Redefinition of process '$name'"
    }

    fun actionIsAlreadyDefined(name: String): String {
        return "Redefinition of action '$name'"
    }

    fun customTypeIsAlreadyDefined(name: String): String {
        return "Redefinition of custom type '$name'"
    }

    fun actionIsAlreadyDeclaredInProc(name: String): String {
        return "Redeclaration of action '$name' in proc"
    }

    fun sysIsAlreadyDefined(name: String): String {
        return "Redefinition of system '$name'"
    }

    fun invalidActionSuffix(name: String, expect: GActionSuffix, actual: GActionSuffix): String {
        return "Invalid action suffix: expect '$name$expect' but got '$name$actual'"
    }

    fun noActionFoundForPostCondition(): String {
        return "No action found for post-condition"
    }

    fun invalidPostCondition(expectedAssert: String, strtAction: String): String {
        return "Invalid post-condition: expected $expectedAssert for $strtAction"
    }

    fun errorProcCallMismatchedArgs(expected: Int, actual: Int): String {
        return "Number of arguments does not match: expected $expected but got $actual"
    }

    fun errorDataDeterministic(fmtAction: String, strSteps: String): String {
        return "Data-determinism is required for input $fmtAction: $strSteps"
    }

    fun errorIamPreCondNotSat(step: String): String {
        return "Pre-condition is not satisfiable at $step"
    }

    fun errorIamPostCondNotSat(step: String): String {
        return "Post-condition is not satisfiable at $step"
    }

    fun errorUnsatisfiablePostCond(formatStep: String): String {
        return "Unsatisfiable post-condition with primed variable at $formatStep"
    }

    fun incompatibleIas(s1: String, s2: String): String {
        return "Systems $s1 and $s2 are incompatible"
    }

    fun missingTools(tools: List<String>): String {
        return "Tools missing in PATH env: ${tools.joinToString(", ")}"
    }

    const val errorPrimeIsOnlyAllowedInAssert = "Primed variable is only allowed in 'assume' or 'guarantee'"

    const val errorPrimeIsForGlobalVarOnly = "Primed reference is for global variable only"

    const val actionNotAllowedInExpr = "Action synchronization cannot be used with other expressions"

    const val elseBranchNotAllowedWithAction = "Else branch is not allowed with action on condition"

    const val cannotComputeType = "Type cannot be computed"

    const val sharedVariableIsNotAllowed = "Shared variable is not allowed in process creation"

    const val disjunctiveMustStatementNotAllowed = "Disjunctive statement is not allowed in IAM (use '#mia' instead)"

    const val mayActionNotAllowed = "May action is not allowed in IAM (use '#mia' instead)"

    const val dataTypesNotAllowed = "Custom data type is not allowed in MIA (use '#iam' instead)"

    const val expressionNotAllowed = "Expression is not allowed in MIA (use '#iam' instead)"

    const val variableNotAllowedInMia = "Variable is not allowed in MIA (use '#iam' instead)"

    const val disjunctiveStatementMustFollowMust = "Disjunctive statement has to follow a must action"

    const val initStateIsPruned = "Invalid automaton (initial state is pruned)"

    const val restrictIsOnlySupportedInIam = "Restrict is only supported in IAM"

    const val scopeIsOnlySupportedInMia = "Scope is only supported in MIA"
}