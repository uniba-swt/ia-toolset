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

package ialib.iam.composition.debug

import ialib.iam.composition.ProductMemState
import org.jgrapht.Graph
import org.jgrapht.graph.builder.GraphTypeBuilder
import org.jgrapht.traverse.DepthFirstIterator

/**
 * find shortest trace from the init state to the error state using a "reverse" graph
 */
class ShortestTraceProvider {

    private val underlying: Graph<String, MemProductTraceRecord> = GraphTypeBuilder.directed<String, MemProductTraceRecord>().allowingSelfLoops(true).allowingMultipleEdges(true).buildGraph()

    var initId: String = ""

    fun addStep(srcId: String, dstId: String, trace: MemProductTraceRecord) {
        // src is the dst state, dst is the source state
        underlying.addVertex(dstId)
        underlying.addVertex(srcId)
        underlying.addEdge(dstId, srcId, trace)
    }

    fun findShortestTrace(state: ProductMemState): List<MemProductTraceRecord>? {
        val iterator = DepthFirstIterator(underlying, state.id)
        for (vertex in iterator) {
            if (vertex == initId) {
                return buildSteps(iterator.stack.filterIsInstance<String>())
            }
        }

        return null
    }

    private fun buildSteps(states: List<String>): List<MemProductTraceRecord> {
        val list = mutableListOf<MemProductTraceRecord>()
        for (i in 0..states.size - 2) {
            list.add(underlying.getEdge(states[i], states[i + 1]))
        }

        return list.reversed()
    }
}