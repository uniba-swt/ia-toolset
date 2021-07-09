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

package ialib.mia

import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import ialib.iam.expr.MActionExpr

class ModalAutomatonBuilder(val name: String, initId: String) {

    private val mapActions= mutableMapOf<String, AutomatonAction>()
    private val mapStates = mutableMapOf<String, ModalState>()
    val initState = ModalState(initId, true)

    init {
        mapStates[initState.name] = initState
    }

    fun build() : ModalAutomaton {
        val inputs = mutableSetOf<AutomatonAction>()
        val outputs = mutableSetOf<AutomatonAction>()
        val internals = mutableSetOf<AutomatonAction>()
        for (action in mapActions.values) {
            when (action.actionType) {
                AutomatonActionType.Input -> inputs.add(action)
                AutomatonActionType.Output -> outputs.add(action)
                AutomatonActionType.Internal -> internals.add(action)
            }
        }

        return ModalAutomaton(name, inputs, outputs, internals, mapStates.values.toSet(), initState)
    }

    fun addMayTransition(srcId: String, action: MActionExpr, dstId: String) {
        addActionIfNeeded(action.action)
        // add or get states
        val src: ModalState = getOrAddState(srcId)
        val dst: ModalState = getOrAddState(dstId)

        // add transitions
        src.addMayTransition(action, dst)
    }

    fun addMustTransition(srcId: String, action: MActionExpr, disjunctiveIds: List<String>) {
        addActionIfNeeded(action.action)
        val src: ModalState = getOrAddState(srcId)
        src.addMustTransition(action, disjunctiveIds.map { d -> getOrAddState(d) })
    }

    fun addActionIfNeeded(action: AutomatonAction) {
        mapActions.getOrPut(action.name) { action }
    }

    private fun getOrAddState(name: String): ModalState {
        return mapStates.getOrPut(name) {
            ModalState(name, false)
        }
    }

    fun markStateAsError(id: String) {
        mapStates[id]?.let { it.isError = true }
    }
}