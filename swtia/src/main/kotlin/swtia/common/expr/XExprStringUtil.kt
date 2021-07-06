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

import swtia.common.expr.XtextToMExprConverter.Companion.convertToMExpr
import swtia.ia.*

object XExprStringUtil {

    fun GAssertStmt.format(): String {
        return "${this.name}(${formatToMExpr(this.expr)})"
    }

    fun GExpr.format(): String {
        return when (this) {
            is GActionExpr -> formatAction(this)
            is GOpExpr -> formatToMExpr(this)
            else -> ""
        }
    }

    private fun formatToMExpr(expr: GOpExpr): String {
        val mexpr = expr.convertToMExpr()
        return mexpr.format()
    }

    private fun formatAction(expr: GActionExpr): String {
        if (expr.isTau) {
            return "tau"
        }

        return expr.action.format() + expr.suffix.format()
    }

    fun GCommonVar.format(): String {
        return this.name
    }

    fun GActionSuffix.format(): String {
        return when (this) {
            GActionSuffix.INPUT -> "?"
            GActionSuffix.OUTPUT -> "!"
        }
    }
}