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

import ialib.dot.DotRenderUtil
import ialib.dot.StringRenderer

class MemDotRenderer {
    private val strRenderer = StringRenderer()
    fun render(automaton: MemAutomaton): String {
        strRenderer.reset()
        strRenderer
                .appendLine("digraph G {")
                .tab().appendLine(String.format("graph[label=\"%s\",compound=true,rankdir=LR]", automaton.name))

        // states and steps
        for (st in automaton.getIterator()) {
            renderState(st)
        }

        // init
        strRenderer.tab().appendLine("$HIDDEN_INIT [shape=none,label=\"\"]")
        strRenderer.tab().appendLine(String.format("%s -> %s [constraint=false]", HIDDEN_INIT, automaton.initState.name))

        // actions
        strRenderer.tab().appendLine(DotRenderUtil.generateActions(automaton.inputActions, automaton.outputActions))

        // close
        strRenderer.appendLine("}")
        return strRenderer.render()
    }

    private fun renderState(state: MemState) {
        // render
        val color = if (state.isError) "red" else "black"
        val shape = if (state.isError) "square" else "oval"
        strRenderer.tab().appendLine("${state.name} [label=\"${state.name}\",color=$color,shape=$shape]")
        for (action in state.actionsSequence) {
            for (step in state.getDstSteps(action)) {
                val label = String.format(
                        "[%s] %s [%s]",
                        step.preCond.format(),
                        action.formatted(),
                        step.postCond.format())
                strRenderer.tab().appendLine(String.format("%s -> %s [label=\"%s\"]",
                        state.name,
                        step.dstState.name,
                        label))
            }
        }
    }

    companion object {
        private const val HIDDEN_INIT = "_qi"
    }
}