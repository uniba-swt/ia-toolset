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

package ialib.debug.json

import ialib.iam.MemStep
import ialib.iam.expr.MExpr
import ialib.iam.expr.MLocation

/**
 * This is used for Debug Adapter Protocol (converted to JSON object)
 */
class TextLocationItem(val text: String, val locations: List<MLocation>, val children: List<TextLocationItem>, val id: String = "") {
    companion object {
        fun MemStep.toTextLocationItem(): TextLocationItem {
            val exprs = listOf(this.preCond, this.action, this.postCond)
            val loc = exprs.map { ex -> ex.mergeLocation() }.filter { l -> !l.isEmpty() }
            val children = exprs.map { ex -> ex.toTextLocationItem() }
            return TextLocationItem(this.toString(), loc, children, this.id)
        }

        private fun MExpr.toTextLocationItem(): TextLocationItem {
            return TextLocationItem(this.toString(), listOf(this.mergeLocation()).filter { l -> !l.isEmpty() }, emptyList())
        }
    }
}