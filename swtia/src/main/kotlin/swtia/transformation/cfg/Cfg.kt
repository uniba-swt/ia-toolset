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
import org.jgrapht.Graph
import org.jgrapht.graph.builder.GraphTypeBuilder
import org.jgrapht.traverse.DepthFirstIterator

/**
 * Generate Control-Flow Graph for both IAM and MIA
 */
open class Cfg<V: CfgVertex, E: CfgEdge>(var name: String, val actions: List<AutomatonAction>) {

    private val underlying: Graph<V, E> = GraphTypeBuilder.directed<V, E>().allowingSelfLoops(true).allowingMultipleEdges(true).buildGraph()

    var init: V? = null
        set(value) {
            field = value
            if (field != null) {
                underlying.addVertex(field)
            }
        }

    fun addEdge(src: V, dst: V, edge: E) {

        // underlying
        underlying.addVertex(src)
        underlying.addVertex(dst)
        underlying.addEdge(src, dst, edge)
    }

    fun outgoingEdgesOf(src: V): Collection<E> {
        return underlying.outgoingEdgesOf(src)
    }

    fun incomingEdgesOf(dst: V): Collection<E> {
        return underlying.incomingEdgesOf(dst)
    }

    private fun removeVertex(vertex: V) {
        underlying.removeVertex(vertex)
    }

    private fun getEdgeSrc(edge: E): V {
        return underlying.getEdgeSource(edge)
    }

    fun getEdgeDst(edge: E): V {
        return underlying.getEdgeTarget(edge)
    }

    fun addVertex(vertex: V) {
        underlying.addVertex(vertex)
    }

    fun getIterator(): DepthFirstIterator<V, E> {
        return DepthFirstIterator(underlying, init)
    }

    fun mergeVertices(fst: V, snd: V, edgeCloneProvider: (E) -> E) {
        // move edges
        val edges = incomingEdgesOf(fst).toList()
        for (edge in edges) {
            addEdge(getEdgeSrc(edge), snd, edgeCloneProvider(edge))
        }

        // remove
        removeVertex(fst)

        // update init
        if (init == fst) {
            init = snd
        }
    }
}