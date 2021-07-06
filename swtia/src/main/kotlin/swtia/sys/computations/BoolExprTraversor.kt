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

package swtia.sys

import com.google.inject.Inject
import swtia.common.expr.ExprTraversor
import swtia.ia.*
import swtia.validation.typing.TypeChecker

class BoolExprTraversor: ExprTraversor<Boolean>() {

    @Inject
    private lateinit var intTraversor: IntExprTraversor

    @Inject
    private lateinit var typeChecker: TypeChecker

    @Inject
    private lateinit var customTypeExprTraversor: CustomTypeExprTraversor

    override fun travelOr(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        return travel(leftExpr) || travel(rightExpr)
    }

    override fun travelAnd(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        return travel(leftExpr) && travel(rightExpr)
    }

    override fun travelEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        return travelEquality(leftExpr, rightExpr) { obj1, obj2 -> obj1 == obj2 }
    }

    override fun travelNotEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        return travelEquality(leftExpr, rightExpr) { obj1, obj2 -> obj1 != obj2 }
    }

    private fun travelEquality(leftExpr: GOpExpr, rightExpr: GOpExpr, provider: (Any, Any) -> Boolean): Boolean {
        // support all
        val dataType = typeChecker.travel(leftExpr)
        return when {
            dataType.isInt -> provider(intTraversor.travel(leftExpr), intTraversor.travel(rightExpr))
            dataType.isBool -> provider(this.travel(leftExpr), this.travel(rightExpr))
            else -> provider(customTypeExprTraversor.compute(leftExpr), customTypeExprTraversor.compute(rightExpr))
        }
    }

    override fun travelGreater(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        return intTraversor.travel(leftExpr) > intTraversor.travel(rightExpr)
    }

    override fun travelLess(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        return intTraversor.travel(leftExpr) < intTraversor.travel(rightExpr)
    }

    override fun travelGreaterOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        return intTraversor.travel(leftExpr) >= intTraversor.travel(rightExpr)
    }

    override fun travelLessOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        return intTraversor.travel(leftExpr) <= intTraversor.travel(rightExpr)
    }

    override fun travelAdd(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        error("operator cannot be computed: +")
    }

    override fun travelSubtract(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        error("operator cannot be computed: -")
    }

    override fun travelMultiply(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        error("operator cannot be computed: *")
    }

    override fun travelDivision(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        error("operator cannot be computed: /")
    }

    override fun travelMod(leftExpr: GOpExpr, rightExpr: GOpExpr): Boolean {
        error("operator cannot be computed: %")
    }

    override fun travelUnaryNot(parent: GExpr, expr: GOpExpr): Boolean {
        return ! travel(expr)
    }

    override fun travelUnaryPlus(parent: GExpr, expr: GOpExpr): Boolean {
        error("operator cannot be computed: +")
    }

    override fun travelUnaryMinus(parent: GExpr, expr: GOpExpr): Boolean {
        error("operator cannot be computed: -")
    }

    override fun travelAction(action: GActionExpr): Boolean {
        error("action cannot be computed")
    }

    override fun travelVarRef(expr: GIdRefExpr, decl: GVarDecl): Boolean {
        // default value
        return false
    }

    override fun travelParamRef(expr: GIdRefExpr, param: GProcParam): Boolean {
        error("param cannot be computed: ${param.name}")
    }

    override fun travelBool(literal: GLiteral, value: GBooleanConst): Boolean {
        return value == GBooleanConst.TRUE
    }

    override fun travelInt(literal: GLiteral, value: Int): Boolean {
        error("int cannot be computed: $value")
    }

    override fun travelCustomType(expr: GIdRefExpr, data: GCustomType, member: GCustomTypeMem): Boolean {
        error("custom type cannot be computed: $data :: $member")
    }
}