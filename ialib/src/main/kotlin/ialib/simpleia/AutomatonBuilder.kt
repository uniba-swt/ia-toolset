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

package ialib.simpleia

import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import java.util.*

class AutomatonBuilder(private val name: String) {
    private val inputs: MutableSet<AutomatonAction>
    private val outputs: MutableSet<AutomatonAction>
    private val internals: MutableSet<AutomatonAction>
    private val states: MutableSet<AutomatonState>
    private val stateIds: MutableSet<String>
    private val setActionName: MutableSet<String>
    var initState: AutomatonState? = null

    fun hasState(state: AutomatonState): Boolean {
        return states.contains(state)
    }

    fun addStateIfNeeded(state: AutomatonState) {
        if (stateIds.contains(state.name)) return
        stateIds.add(state.name)
        states.add(state)
        if (state.isInitial) initState = state
    }

    fun removeState(state: AutomatonState) {
        if (state.equals(initState)) {
            initState = null
        }
        stateIds.remove(state.name)
        states.remove(state)
    }

    fun addStep(srcState: AutomatonState, action: AutomatonAction, dstState: AutomatonState): AutomatonBuilder {
        if (!states.contains(srcState) || !states.contains(dstState)) return this
        if (!inputs.contains(action) && !outputs.contains(action) && !internals.contains(action)) return this

        // add step
        srcState.addStep(action, dstState)
        return this
    }

    fun setInitState(initState: AutomatonState?): AutomatonBuilder {
        this.initState = initState
        return this
    }

    fun build(): Automaton? {
        return if (initState == null) null else Automaton(name, inputs, outputs, internals, states, initState!!)
    }

    fun addAction(action: AutomatonAction): AutomatonBuilder {
        if (!setActionName.contains(action.name)) {
            when (action.actionType) {
                AutomatonActionType.Input -> inputs.add(action)
                AutomatonActionType.Output -> outputs.add(action)
                else -> internals.add(action)
            }
            setActionName.add(action.name)
        }
        return this
    }

    init {
        inputs = HashSet()
        outputs = HashSet()
        internals = HashSet()
        states = HashSet()
        stateIds = HashSet()
        setActionName = HashSet()
    }
}