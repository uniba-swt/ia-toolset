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
import ialib.iam.MemAutomaton
import ialib.iam.MemAutomatonBuilder
import ialib.iam.expr.MActionExpr
import ialib.iam.expr.MExpr
import org.jgrapht.graph.builder.GraphTypeBuilder

class IamGraph(val name: String, var initId: String) {
    private val underlying = GraphTypeBuilder.directed<String, IamEdge>().allowingMultipleEdges(true).allowingSelfLoops(true).buildGraph()

    val actions = mutableListOf<AutomatonAction>()

    val errorStates = mutableSetOf<String>()

    fun createAutomaton(): MemAutomaton {
        val builder = MemAutomatonBuilder(name, initId)

        // add all actions first
        actions.forEach { a -> builder.addActionIfNeeded(a) }

        // add set
        val set = underlying.edgeSet()
        for (edge in set) {
            val src = underlying.getEdgeSource(edge)
            val dst = underlying.getEdgeTarget(edge)
            builder.addTransition(src, dst, edge.action, edge.preCond, edge.postCond)
        }

        // update error states
        for (errorState in errorStates) {
            builder.markStateAsError(errorState)
        }

        return builder.build()
    }

    fun addTransition(
        isInit: Boolean,
        srcId: String,
        dstId: String,
        preCond: MExpr,
        action: MActionExpr,
        postCond: MExpr
    ) {
        if (isInit) {
            initId = srcId
        }

        val edge = IamEdge(action, preCond, postCond)
        underlying.addVertex(srcId)
        underlying.addVertex(dstId)
        underlying.addEdge(srcId, dstId, edge)
    }

    fun mergeVertices(fst: String, snd: String) {
        if (underlying.containsVertex(fst) && underlying.containsVertex(snd)) {
            underlying.incomingEdgesOf(fst)
                .forEach { edge -> underlying.addEdge(underlying.getEdgeSource(edge), snd, edge.clone()) }
            underlying.outgoingEdgesOf(fst)
                .forEach { edge -> underlying.addEdge(snd, underlying.getEdgeTarget(edge), edge.clone()) }

            if (fst == initId) {
                initId = snd
            }

            // remove
            underlying.removeVertex(fst)
        }
    }

    fun markStateAsError(id: String) {
        errorStates.add(id)
    }
}