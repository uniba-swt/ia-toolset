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

package ialib.core.pruner

import ialib.iam.travel.QueueProcessor
import org.jgrapht.Graph
import org.jgrapht.graph.builder.GraphTypeBuilder

/**
 * Build a directed graph for states with autonomous edges only, edge value is the src vertex
 * Add a special error state as init
 * All error states connected to the init state
 *
 *
 * To compute all indirect error states:
 * - Reverse graph from the init state
 * - All reachable states are error states
 */
class ErrorStateScanner {

    private val init = "__ERR_INIT_"

    private val graph: Graph<String, String> = GraphTypeBuilder.directed<String, String>().allowingSelfLoops(true).allowingMultipleEdges(true).buildGraph().apply {
        addVertex(init)
    }

    fun maskError(name: String) {
        addEdge(name, init)
    }

    fun addEdge(src: String, dst: String) {
        graph.addVertex(src)
        graph.addVertex(dst)
        graph.addEdge(src, dst, src)
    }

    /**
     * compute error states, mark errors state as initial state
     */
    fun compute(): Set<String> {
        // reverse
        val set = mutableSetOf<String>()
        val processor = QueueProcessor<String>( { s -> s}, { item, pr ->
            // find incoming
            val edges = graph.incomingEdgesOf(item)
            for (edge in edges) {
                // edge value is the src
                set.add(edge)
                pr.queue(edge)
            }
        } )
        processor.start(init)
        return set
    }
}