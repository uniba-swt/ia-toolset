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

package ialib.iam.expr.solver

import ialib.iam.expr.MDecl
import ialib.iam.expr.MExpr
import ialib.iam.expr.MLogicBinExpr
import ialib.iam.expr.MLogicOp
import ialib.iam.expr.translator.ExprToSmtLibTranslator
import ialib.iam.expr.translator.MExprHelper.computeBoolConst
import ialib.solvers.Z3Solver
import org.apache.log4j.Logger

class DefaultSmtSolver {

    private val translator = ExprToSmtLibTranslator()

    private val solver = Z3Solver.default

    fun checkMissingTools(): List<String> {
        return solver.checkMissingTools()
    }

    fun solveForAllImplies(lhs: MExpr, rhs: MExpr): Boolean {

        val bodyExpr = MLogicBinExpr(lhs, rhs, MLogicOp.IMPLIES)
        val mapDecls = DeclCollector.collect(bodyExpr)
        val body = translator.buildSmt(mapDecls, bodyExpr)
        val declsValue = mapDecls.values
        val text: String = if (declsValue.isEmpty()) {
            body
        } else {
            // (forall ((x Int)) (=> (> x 0) (>= x 0)))
            val params = "(" + declsValue.joinToString(" ") { decl -> "(${formatDecl(decl)})" } + ")"
            "(forall $params $body)"
        }

        return solveExpr(setOf(), text)
    }

    fun solveAnd(lhs: MExpr, rhs: MExpr): Boolean {
        return solveAnd(listOf(lhs, rhs))
    }

    fun solveAnd(exprs: List<MExpr>): Boolean {
        // if one of the expr is const => SAT/UNSAT if true/fase
        for (expr in exprs) {
            val const = expr.computeBoolConst()
            if (const != null) {
                logger.debug("quick solve '$expr': '$const'")
                return const
            }
        }

        // solve with smt solver
        return solve(MExpr.and(exprs))
    }

    fun solve(expr: MExpr): Boolean {

        // quick computation if the expr is const
        val const = expr.computeBoolConst()
        if (const != null) {
            logger.debug("quick solve '$expr': '$const'")
            return const
        }

        // solve with SMT
        val mapDecls = DeclCollector.collect(expr)
        return solveExpr(mapDecls.values, translator.buildSmt(mapDecls, expr))
    }

    private fun solveExpr(decls: Collection<MDecl>, exprText: String): Boolean {
        val strBuilder = StringBuilder()

        // headers
        for (decl in decls) {
            strBuilder.appendLine("(declare-const ${formatDecl(decl)})")
        }

        // body
        strBuilder.appendLine("(assert $exprText)")

        // bottom
        strBuilder.append("(check-sat)")
        return solver.solve(strBuilder.toString())
    }

    private fun formatDecl(decl: MDecl): String {
        val sort = when {
            decl.dataType.isBool -> "Bool"
            else -> "Int"
        }
        return "${translator.translateDecl(decl, false)} $sort"
    }

    companion object {
        private val logger = Logger.getLogger(DefaultSmtSolver::class.java)
    }
}