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

import ialib.iam.expr.MActionExpr
import ialib.iam.expr.MExpr
import ialib.iam.expr.MLocation
import swtia.common.expr.ConversionUtil.convertToMDecl
import swtia.common.expr.ConversionUtil.toMDataType
import swtia.ia.*
import swtia.util.InternalIaException
import swtia.util.ResourceUtil.convertToMAction
import swtia.util.ResourceUtil.getLocation

/**
 * Convert Xtext expr to data class expr
 */
class XtextToMExprConverter(private val mapArgs: Map<GProcParam, MExpr>): ExprTraversor<MExpr>() {

    constructor(): this(emptyMap())

    companion object {
        fun GExpr.convertToMExpr(): MExpr {
            return XtextToMExprConverter().convert(this)
        }
    }

    fun convert(src: GExpr): MExpr {
        return travel(src)
    }

    override fun travelOr(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.or(convert(leftExpr), convert(rightExpr))
    }

    override fun travelAnd(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.and(convert(leftExpr), convert(rightExpr))
    }

    override fun travelEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.equal(convert(leftExpr), convert(rightExpr))
    }

    override fun travelNotEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.notEqual(convert(leftExpr), convert(rightExpr))
    }

    override fun travelGreater(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.gt(convert(leftExpr), convert(rightExpr))
    }

    override fun travelLess(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.lt(convert(leftExpr), convert(rightExpr))
    }

    override fun travelGreaterOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.ge(convert(leftExpr), convert(rightExpr))
    }

    override fun travelLessOrEqual(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.le(convert(leftExpr), convert(rightExpr))
    }

    override fun travelAdd(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.add(convert(leftExpr), convert(rightExpr))
    }

    override fun travelSubtract(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.sub(convert(leftExpr), convert(rightExpr))
    }

    override fun travelMultiply(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.mul(convert(leftExpr), convert(rightExpr))
    }

    override fun travelDivision(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.div(convert(leftExpr), convert(rightExpr))
    }

    override fun travelMod(leftExpr: GOpExpr, rightExpr: GOpExpr): MExpr {
        return MExpr.mod(convert(leftExpr), convert(rightExpr))
    }

    override fun travelUnaryNot(parent: GExpr, expr: GOpExpr): MExpr {
        return MExpr.not(convert(expr), parent.getLocation())
    }

    override fun travelUnaryPlus(parent: GExpr, expr: GOpExpr): MExpr {
        // there should be no change for plus unary
        return convert(expr)
    }

    override fun travelUnaryMinus(parent: GExpr, expr: GOpExpr): MExpr {
        // convert to -1 * expr
        return MExpr.mul(MExpr.constInt(-1, MLocation.empty()), convert(expr))
    }

    override fun travelAction(action: GActionExpr): MExpr {
        if (action.action is GProcParam) {
            return mapArgs[action.action] ?: throw InternalIaException("missing argument")
        }

        return MActionExpr(action.convertToMAction(), action.isMay, action.getLocation())
    }

    override fun travelVarRef(expr: GIdRefExpr, decl: GVarDecl): MExpr {
        return MExpr.declRef(decl.convertToMDecl(), expr.isVarPrime, expr.getLocation())
    }

    override fun travelParamRef(expr: GIdRefExpr, param: GProcParam): MExpr {
        return mapArgs[param] ?: throw InternalIaException("missing argument")
    }

    override fun travelBool(literal: GLiteral, value: GBooleanConst): MExpr {
        return MExpr.constBool(value == GBooleanConst.TRUE, literal.getLocation())
    }

    override fun travelInt(literal: GLiteral, value: Int): MExpr {
        return MExpr.constInt(value, literal.getLocation())
    }

    override fun travelCustomType(expr: GIdRefExpr, data: GCustomType, member: GCustomTypeMem): MExpr {
        return MExpr.constCustomType(data.toMDataType(), member.name, expr.getLocation())
    }
}