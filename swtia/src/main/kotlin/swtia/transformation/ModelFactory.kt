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

package swtia.transformation

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import swtia.ia.*
import swtia.util.ResourceUtil

object ModelFactory {

    val factory = ResourceUtil.factory()

    fun createBranch(expr: GExpr, stmt: GStmt): GCaseBranch {
        val obj = factory.createGCaseBranch()
        obj.expr = expr
        obj.stmt = stmt
        return obj
    }

    fun not(expr: GOpExpr): GExpr {
        val tmp = if (expr is GParenthesizedExpr) {
            expr
        } else {
            factory.createGParenthesizedExpr().also {
                it.expr = expr
            }
        }

        return factory.createGUnaryExpr().also {
            it.expr = tmp
            it.op = GOpType.NOT
        }
    }

    fun createGotoStmt(loopLabelStmt: GLabeledStmt): GGotoStmt {
        return factory.createGGotoStmt().also {
            it.label = loopLabelStmt
        }
    }

    fun replace(old: EObject, new: EObject) {
        EcoreUtil2.replace(old, new)
    }

    fun <T: EObject> copy(obj: T): T {
        return EcoreUtil2.copy(obj)
    }

    fun GOpExpr.createExprStmt(): GExprStmt {
        return factory.createGExprStmt().also { it.expr = this }
    }
}