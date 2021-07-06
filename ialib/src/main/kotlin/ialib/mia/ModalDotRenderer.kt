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

import ialib.dot.DotRenderUtil
import ialib.dot.StringRenderer

class ModalDotRenderer {

    private val strRenderer = StringRenderer()

    fun render(automaton: ModalAutomaton): String {

        // prepare
        strRenderer.reset()
        strRenderer.appendLine("digraph G {")
            .tab()
            .appendLine("graph[label=\"${automaton.name}\",compound=true,rankdir=LR]")

        for (state in automaton.getIterator()) {
            val (shape, color) = if (state.isError) Pair("rect","red") else Pair("oval","black")
            strRenderer.tab().appendLine("${state.name} [shape=$shape,color=$color]")
            appendMaySteps(state, state.getAllMaySteps())
            appendMustSteps(state, state.getAllMustSteps())
        }

        // init step
        val HIDDEN_INIT = "_qi"
        strRenderer.tab().appendLine("$HIDDEN_INIT [shape=none,label=\"\"]")
        strRenderer.tab().appendLine(String.format("%s -> %s [constraint=false]", HIDDEN_INIT, automaton.initState.name))

        // input/output
        strRenderer.tab().appendLine(DotRenderUtil.generateActions(automaton.inputActions, automaton.outputActions))

        // close
        strRenderer.appendLine("}")

        return strRenderer.toString()
    }

    private fun appendMaySteps(
        state: ModalState,
        steps: Collection<List<ModalStep>>
    ) {
        val style = "dashed"
        for (destList in steps) {
            for (dest in destList) {
                strRenderer.tab().tab()
                    .appendLine("${state.name} -> ${dest.states.first().name} [label=\"${dest.action.action.formatted()}\",style=$style]")
            }
        }
    }

    private fun appendMustSteps(
        state: ModalState,
        steps: Collection<List<ModalStep>>
    ) {
        val style = "solid"
        val srcNode = state.name
        for (disjunctiveList in steps) {

            for ((i, step) in disjunctiveList.withIndex()) {
                val states = step.states
                if (states.size == 1) {
                    strRenderer.tab().tab().appendLine("$srcNode -> ${states.first().name} [label=\"${step.action.action.formatted()}\",style=$style]")
                } else {
                    // intermediate node for disjunctive must
                    val nodeName = "${state.name}_${step.action.action.name}_$i"
                    strRenderer.tab().tab().appendLine("$nodeName [shape=point]")
                    strRenderer.tab().tab()
                        .appendLine("$srcNode -> $nodeName [label=\"${step.action.action.formatted()}\",style=$style,arrowhead=none]")

                    // for each destination, from node to state
                    for (dst in states) {
                        strRenderer.tab().tab().appendLine("$nodeName -> ${dst.name} [style=$style,color=cyan4]")
                    }
                }
            }
        }
    }
}