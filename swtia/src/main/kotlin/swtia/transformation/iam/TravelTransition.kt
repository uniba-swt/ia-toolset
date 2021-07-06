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

package swtia.transformation.iam

import ialib.core.AutomatonAction
import ialib.iam.expr.MActionExpr
import ialib.iam.expr.MExpr
import ialib.iam.expr.MLocation

/**
 * An IAM transition used for constructing IAM from CFG only
 */
class TravelTransition {

    private var preCond = mutableListOf<MExpr>()
    var action: MActionExpr? = null
    private var postCond = mutableListOf<MExpr>()

    fun hasAction(): Boolean {
        return action != null
    }

    fun reset() {
        preCond.clear()
        action = null
        postCond.clear()
    }

    fun isEmpty(): Boolean {
        return preCond.isEmpty() && action == null && postCond.isEmpty()
    }

    fun addPreCondition(value: MExpr) {
        preCond.add(value)
    }

    fun addPostCondition(value: MExpr) {
        postCond.add(value)
    }

    fun createCondPair(): Pair<MExpr, MExpr> {
        val pre = createAnd(preCond)
        // post-condition is always true if missing
        val post = if (postCond.isEmpty()) {
            MExpr.constBool(true, MLocation.empty())
        } else {
            createAnd(postCond)
        }
        return Pair(pre, post)
    }

    fun format(): String {
        return listOfNotNull(preCond, action, postCond).joinToString(",") { item -> item.toString() }
    }

    override fun toString(): String {
        return format()
    }

    fun clone(): TravelTransition {
        return TravelTransition().also {
            it.preCond = this.preCond.toMutableList()
            it.postCond = this.postCond.toMutableList()
            it.action = this.action
        }
    }

    companion object {
        private fun createAnd(exprs: List<MExpr>): MExpr {
            if (exprs.isEmpty()) {
                return MExpr.constBool(true, MLocation.empty())
            }

            var result = exprs.first()
            for (i in 1 until exprs.size) {
                result = MExpr.and(result, exprs[i])
            }
            return result
        }
    }
}
