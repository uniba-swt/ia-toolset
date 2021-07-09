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

package swtia.transformation.mia

import ialib.mia.ModalAutomatonBuilder
import swtia.sys.mia.MiaSysIa
import swtia.transformation.TransformException
import swtia.transformation.mia.cfg.MiaCfg
import swtia.transformation.mia.cfg.MiaCfgVertex
import swtia.transformation.mia.cfg.MiaCfgVertexType

/**
 * Transform from CFG to MIA
 *
 * At this stage, there are only 2 types of edge: with action and disjunctive
 */
class CfgToMiaTransformer {

    private val DefaultPrefix = "s"

    private var counter = 0

    private var prefix = DefaultPrefix

    fun transform(cfg: MiaCfg): MiaSysIa {

        if (cfg.init == null)
            throw TransformException("Failed to build MIA", emptyList())

        val mapLbToState = mutableMapOf<String, String>()

        // reset
        counter = 0
        prefix = cfg.name.firstOrNull()?.toString() ?: DefaultPrefix

        // create builder
        val initId= getStateId(mapLbToState, cfg.init!!)
        val builder = ModalAutomatonBuilder(cfg.name, initId)
        cfg.actions.forEach { act -> builder.addActionIfNeeded(act.copy()) }

        // iterate over the vertices
        val iterator = cfg.getIterator()
        for (vertex in iterator) {

            // create modal state
            val src= getStateId(mapLbToState, vertex)

            // only consider vertex that has action edge
            val edges = cfg.outgoingEdgesOf(vertex)
            for (edge in edges) {
                if (edge.isAction()) {
                    val action = edge.action!!
                    val isMay = edge.action.isMay
                    val dst = cfg.getEdgeDst(edge)
                    val dstEdges = cfg.outgoingEdgesOf(dst)

                    // check if dst has disjunctive edge (if must action)
                    // isDisjunctive: if one of the edge is is disjunctive edge -> this vertex is a sub-vertex
                    // for disjunctive-must transition
                    if (!isMay && dstEdges.isNotEmpty() && dstEdges.first().isDisjunctive()) {
                        builder.addMustTransition(src, action, dstEdges.map { e ->
                            getStateId(mapLbToState, cfg.getEdgeDst(e))
                        })
                    } else {
                        // normal processing
                        val dstId = getStateId(mapLbToState, dst)
                        if (isMay) {
                            builder.addMayTransition(src, action, dstId)
                        } else {
                            builder.addMustTransition(src, action, listOf(dstId))
                        }
                    }
                }
            }

            // mark error
            if (vertex.type == MiaCfgVertexType.Error) {
                builder.markStateAsError(src)
            }
        }

        // go through each vertex
        return builder.build().let {
            MiaSysIa.of(it.name, it)
        }
    }

    private fun getStateId(mapLbToState: MutableMap<String, String>, vertex: MiaCfgVertex): String {
        return mapLbToState.getOrPut(vertex.name) { newStateId() }
    }

    private fun newStateId(): String {
        return "${prefix}${counter++}"
    }
}