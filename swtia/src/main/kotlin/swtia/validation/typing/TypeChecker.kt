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

package swtia.validation.typing

import swtia.common.expr.ExprTraversor
import swtia.common.expr.TypingUtil.computeDataType
import swtia.ia.*
import swtia.util.InternalIaException
import swtia.util.Ulogger
import swtia.validation.ErrorMessages
import java.lang.Exception

class TypeChecker: ExprTraversor<DataType>() {

    fun compute(expr: GExpr): DataType {
        try {
            return travel(expr)
        } catch (ex: InternalIaException) {
            Ulogger.error(ex.msg)
             throw TypeException(ErrorMessages.cannotComputeType)
        }
    }

    fun ensure(expr: GExpr, expectedType: DataType) {
        val actual = compute(expr)
        if (actual != expectedType) {
            throw TypeException(ErrorMessages.expectType(expectedType, actual))
        }
    }

    fun ensureBoolOrAction(expr: GExpr) {
        val actual = compute(expr)
        if (!actual.isBool && !actual.isAction) {
            throw TypeException(ErrorMessages.expectBoolOrAction(actual.toString()))
        }
    }

    fun expect(expectedType: DataType, actual: DataType) {
        if (expectedType != actual) {
            throw TypeException(ErrorMessages.expectType(expectedType, actual))
        }
    }

    /**
     * compute type for an param
     */
    fun computeType(param: GCommonVar): DataType {
        return when (param) {
            is GVarDecl -> param.computeDataType()
            is GProcParam -> param.computeDataType()
            else -> throw Exception("param is not supported")
        }
    }

    override fun travelOr(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofBool())
        ensure(rightExpr, DataType.ofBool())
        return DataType.ofBool()
    }

    override fun travelAnd(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofBool())
        ensure(rightExpr, DataType.ofBool())
        return DataType.ofBool()
    }

    override fun travelEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensureEqual(compute(leftExpr), compute(rightExpr))
        return DataType.ofBool()
    }

    override fun travelNotEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensureEqual(compute(leftExpr), compute(rightExpr))
        return DataType.ofBool()
    }

    override fun travelGreater(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofBool()
    }

    override fun travelLess(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofBool()
    }

    override fun travelGreaterOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofBool()
    }

    override fun travelLessOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofBool()
    }

    override fun travelAdd(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofInt()
    }

    override fun travelSubtract(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofInt()
    }

    override fun travelMultiply(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofInt()
    }

    override fun travelDivision(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofInt()
    }

    override fun travelMod(leftExpr: GOpExpr, rightExpr: GOpExpr): DataType {
        ensure(leftExpr, DataType.ofInt())
        ensure(rightExpr, DataType.ofInt())
        return DataType.ofInt()
    }

    override fun travelUnaryNot(parent: GExpr, expr: GOpExpr): DataType {
        ensure(expr, DataType.ofBool())
        return DataType.ofBool()
    }

    override fun travelUnaryPlus(parent: GExpr, expr: GOpExpr): DataType {
        ensure(expr, DataType.ofInt())
        return DataType.ofInt()
    }

    override fun travelUnaryMinus(parent: GExpr, expr: GOpExpr): DataType {
        ensure(expr, DataType.ofInt())
        return DataType.ofInt()
    }

    private fun ensureEqual(leftType: DataType, rightType: DataType) {
        // ensure equal
        if (leftType != rightType) {
            throw TypeException(ErrorMessages.expectSameType(leftType, rightType))
        }
    }

    override fun travelAction(action: GActionExpr): DataType {
        return DataType.ofAction()
    }

    override fun travelVarRef(expr: GIdRefExpr, decl: GVarDecl): DataType {
        return decl.computeDataType()
    }

    override fun travelParamRef(expr: GIdRefExpr, param: GProcParam): DataType {
        return param.computeDataType()
    }

    override fun travelBool(literal: GLiteral, value: GBooleanConst): DataType {
        return DataType.ofBool()
    }

    override fun travelInt(literal: GLiteral, value: Int): DataType {
        return DataType.ofInt()
    }

    override fun travelCustomType(expr: GIdRefExpr, data: GCustomType, member: GCustomTypeMem): DataType {
        return DataType.ofCustomType(data.name)
    }
}