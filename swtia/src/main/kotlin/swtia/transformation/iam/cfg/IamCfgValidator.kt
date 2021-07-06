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

package swtia.transformation.iam.cfg

import ialib.iam.expr.MActionExpr
import swtia.transformation.TransformException
import swtia.transformation.mir.IrAssertStmt
import swtia.util.Constants
import swtia.validation.ErrorMessages

/**
 * perform additional validation on CFG
 *
 * 1. Make sure assume and guarantee are used correctly
 */
class IamCfgValidator {

    private var hasAction = false

    @Throws(TransformException::class)
    fun validate(graph: IamCfg) {

        hasAction = false
        graph.init?.let { travelNode(graph, it, CachedAction.none()) }
    }

    private val setVisitedNodes = mutableSetOf<String>()

    @Throws(TransformException::class)
    private fun travelNode(graph: IamCfg, vertex: IamCfgVertex, lastAction: CachedAction) {
        if (setVisitedNodes.contains(vertex.name)) {
            return
        }
        setVisitedNodes.add(vertex.name)

        onNodeVisited(vertex, lastAction)
        for (edge in graph.outgoingEdgesOf(vertex)) {
            // travel edge
            val tmp = lastAction.clone()
            onEdgeVisited(edge, tmp)
            travelNode(graph, graph.getEdgeDst(edge), tmp)
        }

        setVisitedNodes.remove(vertex.name)
    }

    private fun onEdgeVisited(edge: IamCfgEdge, lastAction: CachedAction) {
        // add action
        if (edge.value != null) {
            // push action
            if (edge.value is MActionExpr) {
                lastAction.action = edge.value
                hasAction = true
            } else {
                lastAction.action = null
            }
        }
    }

    @Throws(TransformException::class)
    private fun onNodeVisited(vertex: IamCfgVertex, lastAction: CachedAction) {
        if (vertex.type == IamCfgVertexType.Assert && vertex.assertion != null) {
            validateAssert(vertex.assertion, lastAction)
        }
    }

    @Throws(TransformException::class)
    private fun validateAssert(assertion: IrAssertStmt, lastAction: CachedAction): Boolean {
        // make sure there is an action
        if (lastAction.action == null) {
            throw TransformException(
                ErrorMessages.noActionFoundForPostCondition(),
                assertion.loc
            )
        }

        // make sure last action is valid
        if (assertion.isGuarantee()) {
            if (!(lastAction.isInputOrInternal())) {
                throw TransformException(
                    ErrorMessages.invalidPostCondition(Constants.NameAssume, format(lastAction)),
                    assertion.loc
                )
            }
        } else if (assertion.isAssume()) {
            if (!(lastAction.isOutputOrInternal())) {
                throw TransformException(ErrorMessages.invalidPostCondition(Constants.NameGuarantee, format(lastAction)),
                    assertion.loc
                )
            }
        }

        return true
    }

    private fun format(action: CachedAction): String {
        return action.action?.format() ?: ""
    }
}

internal class CachedAction private constructor(var action: MActionExpr?) {

    fun isInputOrInternal() = action?.isInputOrInternal() ?: false

    fun isOutputOrInternal() = action?.isOutputOrInternal() ?: false

    fun clone(): CachedAction = CachedAction(this.action)

    companion object {
        fun none(): CachedAction = CachedAction(null)
        fun of(action: MActionExpr): CachedAction = CachedAction(action)
    }
}