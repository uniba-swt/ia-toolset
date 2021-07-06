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

package ialib.simpleia.refinement.mcrl2

import ialib.simpleia.Automaton
import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import ialib.simpleia.AutomatonState
import ialib.util.StringUtil.isNotBlank
import ialib.dot.StringRenderer
import java.util.stream.Collectors
import java.util.stream.Stream

class MCrl2ModelBuilder(private val automaton: Automaton) {
    private val renderer = StringRenderer()
    private val visited = mutableSetOf<String>()

    fun build(): String {
        // interface declaration
        val strAct = Stream.concat(automaton.inputActions.stream(), automaton.outputActions.stream()).map(
            AutomatonAction::name).sorted().collect(Collectors.joining(", "))
        if (isNotBlank(strAct)) {
            renderer.appendLine("act $strAct;")
        }

        // proc
        renderer.appendLine("")
        for (state in automaton.states) {
            renderState(state)
        }

        // init
        renderer.appendLine("").appendLine("init " + automaton.initState.name + ";")

        // finish
        return renderer.toString()
    }

    private fun renderState(state: AutomatonState) {
        // stop if visited
        if (visited.contains(state.name)) return
        visited.add(state.name)
        val body: String
        body = if (state.countActions > 0) {
            state.actionsSequence.map { action: AutomatonAction -> renderAction(state, action) }.sorted().joinToString(" + ")
        } else {
            "delta"
        }
        renderer.appendLine("proc " + state.name + " = " + body + ";")
    }

    private fun renderAction(state: AutomatonState, action: AutomatonAction): String {
        val dst: Set<AutomatonState> = state.getDstStates(action)
        var strDst = state.getDstStates(action).stream().map(AutomatonState::name).collect(Collectors.joining(" + "))
        if (dst.size > 1) {
            strDst = "($strDst)"
        }
        val act = if (action.actionType === AutomatonActionType.Internal) "tau" else action.name
        return "$act . $strDst"
    }
}