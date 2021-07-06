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

import swtia.common.expr.ExprTraversor
import swtia.ia.*

class IntExprTraversor: ExprTraversor<Int>() {

    override fun travelOr(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        error("operator cannot be computed: or")
    }

    override fun travelAnd(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        error("operator cannot be computed: and")
    }

    override fun travelEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        error("operator cannot be computed: ==")
    }

    override fun travelNotEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        error("operator cannot be computed: !=")
    }

    override fun travelGreater(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        error("operator cannot be computed: >")
    }

    override fun travelLess(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        error("operator cannot be computed: <")
    }

    override fun travelGreaterOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        error("operator cannot be computed: >=")
    }

    override fun travelLessOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        error("operator cannot be computed: <=")
    }

    override fun travelAdd(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        return travel(leftExpr) + travel(rightExpr)
    }

    override fun travelSubtract(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        return travel(leftExpr) - travel(rightExpr)
    }

    override fun travelMultiply(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        return travel(leftExpr) * travel(rightExpr)
    }

    override fun travelDivision(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        return travel(leftExpr) / travel(rightExpr)
    }

    override fun travelMod(leftExpr: GOpExpr, rightExpr: GOpExpr): Int {
        return travel(leftExpr) % travel(rightExpr)
    }

    override fun travelUnaryNot(parent: GExpr, expr: GOpExpr): Int {
        error("operator cannot be computed: <=")
    }

    override fun travelUnaryPlus(parent: GExpr, expr: GOpExpr): Int {
        return travel(expr)
    }

    override fun travelUnaryMinus(parent: GExpr, expr: GOpExpr): Int {
        return - travel(expr)
    }

    override fun travelAction(action: GActionExpr): Int {
        error("action cannot be computed")
    }

    override fun travelVarRef(expr: GIdRefExpr, decl: GVarDecl): Int {
        // default value
        return 0
    }

    override fun travelParamRef(expr: GIdRefExpr, param: GProcParam): Int {
        error("param cannot be computed: ${param.name}")
    }

    override fun travelBool(literal: GLiteral, value: GBooleanConst): Int {
        error("bool cannot be computed: $value")
    }

    override fun travelInt(literal: GLiteral, value: Int): Int {
        return value
    }

    override fun travelCustomType(expr: GIdRefExpr, data: GCustomType, member: GCustomTypeMem): Int {
        error("custom type cannot be computed: $data :: $member")
    }

}