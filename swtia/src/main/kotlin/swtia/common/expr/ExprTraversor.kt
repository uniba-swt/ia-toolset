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

package swtia.common.expr

import swtia.ia.*
import swtia.util.InternalIaException
import swtia.util.ResourceUtil.parentType

abstract class ExprTraversor<TOut> {
    fun travel(expr: GExpr): TOut{
        if (expr is GOpExpr) {
            return travelOp(expr)
        }

        throw InternalIaException("expr is not supported: $expr")
    }

    private fun travelOp(expr: GOpExpr): TOut {
        return when (expr) {
            // primary
            is GParenthesizedExpr -> travelOp(expr.expr)
            is GLiteralExpr -> {
                when (val literal = expr.literal) {
                    is GBooleanLiteral -> travelBool(literal, literal.value)
                    is GIntLiteral -> travelInt(literal, literal.value)
                    else -> throw InternalIaException("literal is not supported: $$expr")
                }
            }
            is GIdRefExpr -> when (val commonVar = expr.ref) {
                is GVarDecl -> travelVarRef(expr, commonVar)
                is GProcParam -> travelParamRef(expr, commonVar)
                is GCustomTypeMem -> travelCustomType(expr, commonVar.parentType(), commonVar)
                else -> throw InternalIaException("invalid ref: $commonVar")
            }
            is GActionExpr -> travelAction(expr)

            // unary
            is GUnaryExpr -> travelUnary(expr, expr.op, expr.expr)

            // op
            else -> travelBinary(expr.op, expr.leftExpr, expr.rightExpr)
        }
    }

    private fun travelBinary(op: GOpType, leftExpr: GOpExpr?, rightExpr: GOpExpr?): TOut {
        if (leftExpr == null) {
            throw InternalIaException("unexpected null value of leftExpr")
        }

        if (rightExpr == null) {
            throw InternalIaException("unexpected null value of rightExpr")
        }

        return when (op) {
            GOpType.OR -> travelOr(leftExpr, rightExpr)
            GOpType.AND -> travelAnd(leftExpr, rightExpr)
            GOpType.EQUAL -> travelEqual(leftExpr, rightExpr)
            GOpType.NOT_EQUAL -> travelNotEqual(leftExpr, rightExpr)
            GOpType.GREATER -> travelGreater(leftExpr, rightExpr)
            GOpType.LESS -> travelLess(leftExpr, rightExpr)
            GOpType.GREATER_OR_EQUAL -> travelGreaterOrEqual(leftExpr, rightExpr)
            GOpType.LES_OR_EQUAL -> travelLessOrEqual(leftExpr, rightExpr)
            GOpType.ADD -> travelAdd(leftExpr, rightExpr)
            GOpType.SUBTRACT -> travelSubtract(leftExpr, rightExpr)
            GOpType.MULTIPLY -> travelMultiply(leftExpr, rightExpr)
            GOpType.DIVISION -> travelDivision(leftExpr, rightExpr)
            GOpType.MOD -> travelMod(leftExpr, rightExpr)
            else -> throw InternalIaException("op is not supported in binary: $op")
        }
    }

    private fun travelUnary(parent: GExpr, op: GOpType, expr: GOpExpr): TOut {
        return when (op) {
            GOpType.PLUS -> travelUnaryPlus(parent, expr)
            GOpType.MINUS -> travelUnaryMinus(parent, expr)
            GOpType.NOT -> travelUnaryNot(parent, expr)
            else -> throw InternalIaException("op is not supported in unary: $op")
        }
    }

    protected abstract fun travelOr(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelAnd(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelNotEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelGreater(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelLess(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelGreaterOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelLessOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelAdd(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelSubtract(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelMultiply(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelDivision(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelMod(leftExpr: GOpExpr, rightExpr: GOpExpr): TOut

    protected abstract fun travelUnaryNot(parent: GExpr, expr: GOpExpr): TOut

    protected abstract fun travelUnaryPlus(parent: GExpr, expr: GOpExpr): TOut

    protected abstract fun travelUnaryMinus(parent: GExpr, expr: GOpExpr): TOut

    protected abstract fun travelAction(action: GActionExpr): TOut

    protected abstract fun travelVarRef(expr: GIdRefExpr, decl: GVarDecl): TOut

    protected abstract fun travelParamRef(expr: GIdRefExpr, param: GProcParam): TOut

    abstract fun travelBool(literal: GLiteral, value: GBooleanConst): TOut

    abstract fun travelInt(literal: GLiteral, value: Int): TOut

    abstract fun travelCustomType(expr: GIdRefExpr, data: GCustomType, member: GCustomTypeMem): TOut
}