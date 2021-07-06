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

package ialib.iam.expr.translator

import ialib.iam.expr.*

abstract class AbstractExprTranslator<TOut> {

    fun translate(expr: MExpr): TOut {
        return when (expr) {
            is MGroupedExpr -> translateGroup(translate(expr.expr))
            is MNotExpr -> translateNot(translate(expr.expr))
            is MDeclRefExpr -> translateDecl(expr.decl, expr.isPrime)
            is MBoolLiteralExpr -> if (expr.isTrue) createTrue() else createFalse()
            is MIntLiteralExpr -> createNumConst(expr.num)
            is MLogicBinExpr -> translateLoginBin(expr)
            is MCustomTypeLiteralExpr -> translateTypeValue(expr.type, expr.value)
            is MRelationalBinExpr -> translateRelationalBin(expr)
            is MEqualityBinExpr -> translateEqualityBin(expr)
            is MArithBinExpr -> translateArithBin(expr)
            else -> error("Formula is not yet supported at root level: " + expr.javaClass.name)
        }
    }

    private fun translateEqualityBin(expr: MEqualityBinExpr): TOut {
        val lhs = translate(expr.lhs)
        val rhs = translate(expr.rhs)
        return when (expr.op) {
            MEqualityOp.EQ -> translateExprEq(lhs, rhs)
            MEqualityOp.NEQ -> translateExprNeq(lhs, rhs)
        }
    }

    private fun translateLoginBin(expr: MLogicBinExpr): TOut {
        return when (expr.op) {
            MLogicOp.IMPLIES -> translateImplies(translate(expr.lhs), translate(expr.rhs))
            MLogicOp.OR -> translateOr(translate(expr.lhs), translate(expr.rhs))
            MLogicOp.AND -> translateAnd(translate(expr.lhs), translate(expr.rhs))
        }
    }

    private fun translateRelationalBin(expr: MRelationalBinExpr): TOut {
        val lhs = translate(expr.lhs)
        val rhs = translate(expr.rhs)
        return when (expr.op) {
            MRelationalOp.GT -> translateExprGt(lhs, rhs)
            MRelationalOp.GE -> translateExprGe(lhs, rhs)
            MRelationalOp.LT -> translateExprLt(lhs, rhs)
            MRelationalOp.LE -> translateExprLe(lhs, rhs)
        }
    }

    private fun translateArithBin(expr: MArithBinExpr): TOut {
        val lhs = translate(expr.lhs)
        val rhs = translate(expr.rhs)
        return when (expr.op) {
            MArithOp.PLUS -> translatePlus(lhs, rhs)
            MArithOp.MINUS -> translateMinus(lhs, rhs)
            MArithOp.MULTIPLY -> translateMul(lhs, rhs)
            MArithOp.DIVISION -> translateDiv(lhs, rhs)
            MArithOp.MOD -> translateMod(lhs, rhs)
        }
    }

    protected abstract fun translateTypeValue(type: MCustomType, value: String): TOut
    abstract fun translateDecl(decl: MDecl, isPrime: Boolean): TOut
    protected abstract fun createNumConst(num: Int): TOut
    protected abstract fun translatePlus(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateMinus(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateMul(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateDiv(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateMod(lhs: TOut, rhs: TOut): TOut
    protected abstract fun createFalse(): TOut
    protected abstract fun createTrue(): TOut
    protected abstract fun translateNot(content: TOut): TOut
    protected abstract fun translateGroup(content: TOut): TOut
    protected abstract fun translateOr(lhs: TOut, rhs: TOut): TOut
    abstract fun translateImplies(lhs:TOut, rhs: TOut): TOut
    abstract fun translateAnd(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateExprGt(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateExprGe(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateExprEq(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateExprNeq(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateExprLt(lhs: TOut, rhs: TOut): TOut
    protected abstract fun translateExprLe(lhs: TOut, rhs: TOut): TOut
}