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

package ialib.iam.expr

import ialib.iam.expr.MLocation.Companion.mergeLocations

abstract class MExpr internal constructor() {

    abstract fun format(): String

    abstract fun children(): Sequence<MExpr>

    abstract fun getLocations(): Sequence<MLocation>

    override fun toString(): String = format()

    fun mergeLocation(): MLocation {
        return getLocations().iterator().mergeLocations()
    }

    /**
     * factory
     */
    companion object {

        fun or(lhs: MExpr, rhs: MExpr): MExpr {
            return MLogicBinExpr(lhs, rhs, MLogicOp.OR)
        }

        fun and(lhs: MExpr, rhs: MExpr): MExpr {
            return MLogicBinExpr(lhs, rhs, MLogicOp.AND)
        }

        fun and(exprs: List<MExpr>): MExpr {
            if (exprs.isEmpty())
                return constBool(true, MLocation.empty())

            if (exprs.size == 1)
                return exprs.first()

            val head = and(exprs[0], exprs[1])
            if (exprs.size > 2) {
                return and(head, and(exprs.subList(2, exprs.size)))
            }
            return head
        }

        fun not(expr: MExpr, loc: MLocation): MExpr {
            return MNotExpr(expr, loc)
        }

        fun equal(lhs: MExpr, rhs: MExpr): MExpr {
            return MEqualityBinExpr(lhs, rhs, MEqualityOp.EQ)
        }

        fun notEqual(lhs: MExpr, rhs: MExpr): MExpr {
            return MEqualityBinExpr(lhs, rhs, MEqualityOp.NEQ)
        }

        fun gt(lhs: MExpr, rhs: MExpr): MExpr {
            return MRelationalBinExpr(lhs, rhs, MRelationalOp.GT)
        }

        fun lt(lhs: MExpr, rhs: MExpr): MExpr {
            return MRelationalBinExpr(lhs, rhs, MRelationalOp.LT)
        }

        fun ge(lhs: MExpr, rhs: MExpr): MExpr {
            return MRelationalBinExpr(lhs, rhs, MRelationalOp.GE)
        }

        fun le(lhs: MExpr, rhs: MExpr): MExpr {
            return MRelationalBinExpr(lhs, rhs, MRelationalOp.LE)
        }

        fun add(lhs: MExpr, rhs: MExpr): MExpr {
            return MArithBinExpr(lhs, rhs, MArithOp.PLUS)
        }

        fun sub(lhs: MExpr, rhs: MExpr): MExpr {
            return MArithBinExpr(lhs, rhs, MArithOp.MINUS)
        }

        fun mul(lhs: MExpr, rhs: MExpr): MExpr {
            return MArithBinExpr(lhs, rhs, MArithOp.MULTIPLY)
        }

        fun div(lhs: MExpr, rhs: MExpr): MExpr {
            return MArithBinExpr(lhs, rhs, MArithOp.DIVISION)
        }

        fun mod(lhs: MExpr, rhs: MExpr): MExpr {
            return MArithBinExpr(lhs, rhs, MArithOp.MOD)
        }

        fun declRef(decl: MDecl, isPrime: Boolean, loc: MLocation): MExpr {
            return MDeclRefExpr(decl, isPrime, loc)
        }

        fun constInt(num: Int, loc: MLocation): MExpr {
            return MIntLiteralExpr(num, loc)
        }

        fun constBool(isTrue: Boolean, loc: MLocation): MExpr {
            return MBoolLiteralExpr(isTrue, loc)
        }

        fun constCustomType(customType: MCustomType, value: String, loc: MLocation): MExpr {
            return MCustomTypeLiteralExpr(customType, value, loc)
        }
    }
}