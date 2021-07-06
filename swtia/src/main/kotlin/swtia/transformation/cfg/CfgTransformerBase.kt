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

package swtia.transformation.cfg

import ialib.core.AutomatonAction
import ialib.iam.expr.MExpr
import swtia.transformation.mir.*

abstract class CfgTransformerBase<V: CfgVertex, E: CfgEdge, G: Cfg<V, E>>(val proc: IrProc) {

    private val mapLabelNode = mutableMapOf<String, V>()
    private val mapLabelIndex = mutableMapOf<String, Int>()
    private val setVisitedLabel = mutableSetOf<String>()

    fun transform(): G {

        // build all vertices for labels
        for ((i, stmt) in proc.stmts.withIndex()) {
            if (stmt is IrLabelStmt) {
                mapLabelNode[stmt.name] = createVertexLabel(stmt.name)
                mapLabelIndex[stmt.name] = i
            }
        }

        val init = createVertexLabel(newLabel())

        // create graph
        val cfg = createCfg(proc.name, proc.actions)
        cfg.addVertex(init)
        cfg.init = init

        transformStmts(proc.stmts, 0, cfg, init)

        // prune
        newPruner(cfg).prune()
        return cfg

    }

    /**
     * Continue building CFG with the root vertext from provided param "init"
     * New vertex is reached by connecting the root vertex to the new vertex via appropriate edge
     * Sometime, edge can be an empty edge (so-called temporary edge).
     * The empty edges should be pruned later using pruner
     */
    protected fun transformStmts(
        stmts: List<IrStmt>,
        start: Int,
        cfg: G,
        init: V
    ) {
        // go through all other statements
        var last = init
        for (i in start until stmts.size) {
            val stmt = stmts[i]

            // normal statement
            last = when (stmt) {
                is IrLabelStmt -> {
                    val next = mapLabelNode[stmt.name]!!
                    cfg.addEdge(last, next, createEdge(null))

                    // stop here
                    if (setVisitedLabel.contains(stmt.name)) {
                        return
                    } else {
                        setVisitedLabel.add(stmt.name)
                    }

                    next
                }
                is IrGotoStmt -> {
                    transformLabel(stmts, stmt.label, cfg, last, null)
                    return
                }
                is IrExprStmt -> {
                    val next = createVertexLabel(newLabel())
                    cfg.addEdge(last, next, createEdge(stmt.expr))
                    next
                }
                is IrAssertStmt -> {
                    transformAssert(stmt, cfg, last)
                }
                is IrErrorSkipStmt -> {
                    if (stmt.isError) {
                        val next = createVertexError(newLabel())
                        cfg.addEdge(last, next, createEdge(null))
                        // stop if catching error
                        return
                    } else {
                        last
                    }
                }
                else -> last
            }

            // if case stmt
            when (stmt) {
                is IrCaseStmt -> {
                    for ((guard, label) in stmt.arms) {
                        transformLabel(stmts, label, cfg, last, guard)
                    }

                    // stop if matching case
                    return
                }
                is IrDisjunctiveGotoStmt -> {
                    transformDisjunctiveStmt(stmts, stmt, cfg, last)
                    // stop if matching case
                    return
                }
            }
        }
    }

    private fun transformLabel(
        stmts: List<IrStmt>,
        label: IrLabelStmt,
        cfg: G,
        last: V,
        expr: MExpr?
    ) {
        // create temporary for goto
        val next = createVertexLabel(newLabel())
        cfg.addEdge(last, next, createEdge(expr))

        // recursive building branch
        val lbIndex = mapLabelIndex[label.name]!!
        transformStmts(stmts, lbIndex, cfg, next)
    }

    protected abstract fun createCfg(name: String, actionRefs: List<AutomatonAction>): G

    protected abstract fun createVertexLabel(name: String): V

    protected abstract fun createVertexError(label: String): V

    protected abstract fun createEdge(expr: MExpr?): E

    protected abstract fun newPruner(cfg: G): CfgPruner<G, V>

    protected abstract fun transformDisjunctiveStmt(
        stmts: List<IrStmt>,
        stmt: IrDisjunctiveGotoStmt,
        cfg: G,
        last: V
    )

    protected abstract fun transformAssert(stmt: IrAssertStmt, cfg: G, last: V): V

    protected fun getLabelIndex(name: String): Int? {
        return mapLabelIndex[name]
    }

    private var labelCounter = 0

    protected fun newLabel(): String {
        return "_cfg_${labelCounter++}"
    }
}