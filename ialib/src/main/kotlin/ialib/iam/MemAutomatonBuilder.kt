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

package ialib.iam

import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import ialib.iam.expr.MActionExpr
import ialib.iam.expr.MDecl
import ialib.iam.expr.MExpr

class MemAutomatonBuilder(private val name: String, initId: String, decls: Collection<MDecl> = emptyList()) {

    private val initState = MemState(initId, true)
    private val mapActions= mutableMapOf<String, AutomatonAction>()
    val mapStates = mutableMapOf<String, MemState>()
    private val mapDecls = mutableMapOf<String, MDecl>()

    init {
        decls.forEach { d -> addDecl(d) }
        addStateIfNeeded(initState)
    }

    fun build(): MemAutomaton {

        // actions by type
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

        // create ia
        return MemAutomaton(
            name,
            inputs,
            outputs,
            internals,
            mapStates.values.toSet(),
            initState,
            mapDecls.values.toSet()
        )
    }

    fun addActionIfNeeded(action: AutomatonAction) {
        mapActions.getOrPut(action.name) { action }
    }

    fun addTransition(srcId: String, dstState: String, action: MActionExpr, preCond: MExpr, postCond: MExpr): MemStep {

        // add or get states
        val src: MemState = getOrAddState(srcId)
        val dst: MemState = getOrAddState(dstState)

        // action if needed
        addActionIfNeeded(action.action)

        // add transition
        val step = MemStep(preCond, action, postCond, dst)
        src.addStep(action.action, step)
        return step
    }

    private fun addStateIfNeeded(state: MemState): MemState {
        return mapStates.getOrPut(state.name) { state }
    }

    fun addDecl(decl: MDecl) {
        mapDecls.getOrPut(decl.name) { decl }
    }

    private fun getOrAddState(name: String): MemState {
        return mapStates.getOrElse(name) { addStateIfNeeded(MemState(name, false)) }
    }

    fun markStateAsError(id: String) {
        mapStates[id]?.let { it.isError = true }
    }
}