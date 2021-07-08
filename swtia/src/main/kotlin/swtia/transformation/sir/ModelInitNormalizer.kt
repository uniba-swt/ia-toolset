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

package swtia.transformation.sir

import com.google.inject.Inject
import swtia.ia.*
import swtia.sys.computations.ExprComputation
import swtia.transformation.ModelFactory
import swtia.util.ResourceUtil.isDataParam
import swtia.util.Ulogger
import swtia.validation.typing.TypeChecker

class ModelInitNormalizer {

    @Inject
    private lateinit var exprComputation: ExprComputation

    @Inject
    private lateinit var typeChecker: TypeChecker

    fun normalize(init: GModelInit) {
        // introduce temporary variable for statement
        val lst = mutableListOf<GSysStmt>()
        for (item in init.items) {
            when (item) {
                is GSysDeclStmt -> {
                    val expr = item.expr
                    lst.addAll(normalizeRootExpr(expr))
                }
                is GSysExprStmt -> {
                    lst.addAll(normalizeRootExpr(item.expr))
                }
            }
            lst.add(item)
        }
        init.items.clear()
        init.items.addAll(lst)

        // 2nd pass
        val lst2 = normalizeCompositionInList(init.items)
        init.items.clear()
        init.items.addAll(lst2)

        // second pass
        normalizeProcCall(init)
    }

    private fun normalizeCompositionInList(items: List<GSysStmt>): List<GSysStmt> {
        val lst = mutableListOf<GSysStmt>()
        for (item in items) {
            val expr = when (item) {
                is GSysDeclStmt -> {
                    item.expr
                }
                is GSysExprStmt -> {
                    item.expr
                }
                else -> null
            }

            // normalize if composition
            if (expr is GSysBinOpExpr && expr.opType == GSysBinOpType.COMPOSITION) {
                val (prodStmt, pruneExpr) = normalizeCompositionExpr(expr)
                lst.add(prodStmt)
                item.expr = pruneExpr
            }

            // add normal
            lst.add(item)
        }
        return lst
    }

    private fun normalizeCompositionExpr(expr: GSysBinOpExpr): Pair<GSysStmt, GSysPruneExpr> {
        // create product
        val prodExpr = ModelFactory.factory.createGSysBinOpExpr().also {
            it.opType = GSysBinOpType.PRODUCT
            it.param1 = expr.param1
            it.param2 = expr.param2
        }
        val prodDecl = ModelFactory.factory.createGSysDeclStmt().also {
            it.name = newTemp()
            it.expr = prodExpr
        }

        // create prune
        val refProd = ModelFactory.factory.createGSysDeclRefExpr().also { it.decl = prodDecl }
        val pruneExpr = ModelFactory.factory.createGSysPruneExpr().also {
            it.param = refProd
        }
        return Pair(prodDecl, pruneExpr)
    }

    private fun normalizeProcCall(init: GModelInit) {
        init.items.forEach { item ->
            if (item is GSysDeclStmt) {
                val expr = item.expr
                if (expr is GSysProcCallExpr) {
                    replaceArgs(expr)
                }
            }
        }
    }

    private fun replaceArgs(item: GSysProcCallExpr) {
        if (item.args.size > 0) {
            Ulogger.debug { "replace arguments by literal value for proc call: ${item.proc.name}" }
            val lst = mutableListOf<GExpr>()
            val params = item.proc.params
            for ((i, arg) in item.args.withIndex()) {
                val param = params[i]
                if (param.isDataParam()) {
                    val expectedType = typeChecker.computeType(param)
                    lst.add(exprComputation.computeToLiteralExpr(arg, expectedType))
                } else {
                    lst.add(arg)
                }
            }

            // replace
            item.args.clear()
            item.args.addAll(lst)
        }
    }

    private fun normalizeRootExpr(expr: GSysExpr): List<GSysDeclStmt> {
        return when (expr) {
            is GSysBinOpExpr -> normalizeExpr(expr.param1) + normalizeExpr(expr.param2)
            is GSysPruneExpr -> normalizeExpr(expr.param)
            is GSysRestrictExpr -> normalizeExpr(expr.param)
            is GSysScopeExpr -> normalizeExpr(expr.param)
            else -> emptyList()
        }
    }

    private fun normalizeExpr(expr: GSysExpr): List<GSysDeclStmt> {
        if (expr !is GSysDeclRefExpr) {
            val lst = mutableListOf<GSysDeclStmt>()

            // create temporary and ref
            val decl = ModelFactory.factory.createGSysDeclStmt().also {
                it.name = newTemp()
                it.expr = ModelFactory.copy(expr)
            }
            val refExpr = ModelFactory.factory.createGSysDeclRefExpr().also { it.decl = decl }
            ModelFactory.replace(expr, refExpr)

            // recursive
            lst.addAll(normalizeRootExpr(decl.expr))

            // normalize expr
            lst.add(decl)

            return lst
        }

        return emptyList()
    }

    private var counter = 0
    private fun newTemp(): String {
        return "_t${counter++}"
    }
}