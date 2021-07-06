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

import ialib.iam.expr.MActionExpr
import ialib.iam.expr.MExpr
import swtia.sys.iam.SysIa
import swtia.transformation.iam.cfg.IamCfgEdge
import swtia.transformation.iam.cfg.IamCfgVertex
import swtia.transformation.iam.cfg.IamCfgVertexType
import swtia.transformation.iam.cfg.IamCfg
import swtia.util.InternalIaException
import swtia.util.Ulogger

class CfgToIamTransformation {

    private lateinit var initId: String

    private lateinit var iam: IamGraph

    private val DefaultPrefix = "s"

    private var counter = 0

    private var prefix = DefaultPrefix

    /**
     * build IAM from CFG
     */
    fun transform(cfgGraph: IamCfg): SysIa {

        // reset
        counter = 0
        prefix = cfgGraph.name.firstOrNull()?.toString() ?: DefaultPrefix

        // prepare
        initId = newStateId()

        // init builder
        iam = IamGraph(cfgGraph.name, initId)
        cfgGraph.actions.forEach { action -> iam.actions.add(action.copy()) }
        cfgGraph.init?.let { dfsNode(cfgGraph, it, initId, TravelTransition()) }
        return SysIa(iam.createAutomaton())
    }

    private val mapVisitedNodes = mutableMapOf<String, String>()

    private fun dfsNode(graph: IamCfg, node: IamCfgVertex, id: String, transition: TravelTransition) {
        if (mapVisitedNodes.contains(node.name)) {
            onFinishedPath(transition, node.type, id, mapVisitedNodes[node.name]!!)
            return
        }

        // check vertex
        var srcId = onNodeVisited(transition, node, id)

        // check edges
        val edges = graph.outgoingEdgesOf(node)

        // conclude if having mutiple branches
        if (edges.size > 1) {
            srcId = concludeTransition(transition, srcId, newStateId())
        } else if (edges.isEmpty()) {
            Ulogger.debug { "travel node finished: $mapVisitedNodes at $node" }
            val dst = newStateId()
            onFinishedPath(transition, node.type, srcId, dst)
            srcId = dst
        } else if (edges.size == 1) {
            // conclude if has action and the next one is an action or pre-cond
            if (transition.hasAction() && edges.first().value != null) {
                srcId = concludeTransition(transition, srcId, newStateId())
            }
        }

        mapVisitedNodes[node.name] = srcId

        // travel
        for (edge in edges) {
            // travel edge
            val tmpTransition = transition.clone()
            val newId = onEdgeVisited(edge, srcId, tmpTransition)
            dfsNode(graph, graph.getEdgeDst(edge), newId, tmpTransition)
        }

        mapVisitedNodes.remove(node.name)
    }

    private fun onEdgeVisited(edge: IamCfgEdge, srcId: String, transition: TravelTransition): String {
        if (edge.hasValue()) {

            if (edge.value is MActionExpr) {

                // finish if previous is an action, pre and post condition can be ignored (const true)
                val newId: String? = if (transition.hasAction()) {
                    concludeTransition(transition, srcId, newStateId())
                } else { null }

                // update action
                transition.action = edge.value

                // return new id
                if (newId != null) {
                    return newId
                }

                return srcId
            }

            if (edge.value is MExpr) {

                // finish if previous is an action, since this op expr is always the precond for next transition
                val newId: String? = if (transition.hasAction()) {
                    concludeTransition(transition, srcId, newStateId())
                } else { null }

                // update pre-condition
                transition.addPreCondition(edge.value)

                if (newId != null) {
                    return newId
                }
            }
        }

        return srcId
    }

    private fun onFinishedPath(transition: TravelTransition, type: IamCfgVertexType, srcId: String, dstId: String) {
        if (!transition.isEmpty()) {
            concludeTransition(transition, srcId, dstId)
            if (type == IamCfgVertexType.Error) {
                markStateAsError(dstId)
            }
        } else {
            Ulogger.debug { "merge vertices: $srcId => $dstId" }
            iam.mergeVertices(srcId, dstId)
        }
    }

    private fun onNodeVisited(transition: TravelTransition, nodeData: IamCfgVertex, srcId: String): String {
        // to support mutiple post conditions, simply add it into list
        if (nodeData.type == IamCfgVertexType.Assert) {
            // make sure action is ready
            if (!transition.hasAction()) {
                throw InternalIaException("CFG to IAM is failed, no action found for assertion")
            }

            // conclude
            transition.addPostCondition(nodeData.assertion!!.expr)
        }

        return srcId
    }

    private fun concludeTransition(transition: TravelTransition, srcId: String, dstId: String): String {
        Ulogger.debug { "concludeTransition: $srcId -> $dstId [ ${transition.format()} ]" }
        return if (transition.hasAction()) {

            // add new transition
            val condPair = transition.createCondPair()
            iam.addTransition(initId == srcId, srcId, dstId, condPair.first, transition.action!!, condPair.second)

            // clear
            transition.reset()
            dstId
        } else {
            srcId
        }
    }

    private fun markStateAsError(id: String) {
        iam.markStateAsError(id)
    }

    private fun newStateId(): String {
        return "${prefix}${counter++}"
    }
}