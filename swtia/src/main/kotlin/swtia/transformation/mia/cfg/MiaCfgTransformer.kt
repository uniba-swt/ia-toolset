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

package swtia.transformation.mia.cfg

import ialib.core.AutomatonAction
import ialib.iam.expr.MActionExpr
import ialib.iam.expr.MExpr
import swtia.transformation.TransformException
import swtia.transformation.cfg.CfgPruner
import swtia.transformation.cfg.CfgTransformerBase
import swtia.transformation.mir.IrAssertStmt
import swtia.transformation.mir.IrDisjunctiveGotoStmt
import swtia.transformation.mir.IrProc
import swtia.transformation.mir.IrStmt

class MiaCfgTransformer(proc: IrProc): CfgTransformerBase<MiaCfgVertex, MiaCfgEdge, MiaCfg>(proc) {

    override fun transformAssert(stmt: IrAssertStmt, cfg: MiaCfg, last: MiaCfgVertex): MiaCfgVertex {
        throw TransformException("GAssertStmt is not supported in MIA", stmt.loc)
    }

    override fun newPruner(cfg: MiaCfg): CfgPruner<MiaCfg, MiaCfgVertex> {
        return MiaCfgPruner(cfg)
    }

    override fun createCfg(name: String, actionRefs: List<AutomatonAction>): MiaCfg {
        return MiaCfg(name, actionRefs)
    }

    override fun createVertexLabel(name: String): MiaCfgVertex {
        return MiaCfgVertex.label(name)
    }

    override fun createEdge(expr: MExpr?): MiaCfgEdge {
        if (expr == null)
            return MiaCfgEdge.empty()

        // only accept expression with action
        if (expr is MActionExpr)
            return MiaCfgEdge.action(expr)

        throw TransformException("not supported edge: $expr", expr.getLocations().toList())
    }

    override fun transformDisjunctiveStmt(
        stmts: List<IrStmt>,
        stmt: IrDisjunctiveGotoStmt,
        cfg: MiaCfg,
        last: MiaCfgVertex,
    ) {
        for (label in stmt.labels) {
            // create disjunctive edge
            val next = createVertexLabel(newLabel())
            val edge = MiaCfgEdge.disjunctive()
            cfg.addEdge(last, next, edge)

            // recursive building branch
            val lbIndex = getLabelIndex(label.name)!!
            transformStmts(stmts, lbIndex, cfg, next)
        }
    }

    override fun createVertexError(label: String): MiaCfgVertex {
        return MiaCfgVertex.error(label)
    }
}