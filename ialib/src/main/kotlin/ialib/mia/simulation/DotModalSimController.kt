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

package ialib.mia.simulation

import ialib.core.services.TextFileWriter
import ialib.dot.StringRenderer
import ialib.mia.refinement.ModalSimGraph
import ialib.util.ColorUtil
import ialib.util.StringUtil

class DotModalSimController(
    private val genFsa: TextFileWriter,
    private val graph: ModalSimGraph
) {

    private val renderer: StringRenderer = StringRenderer()

    private val filename = "MIA_Simulation_${graph.name}.dot"

    fun exec(): String {
        prepare()
        for (state in graph.states) {
            processItem(state)
        }
        finish()
        val dot = renderer.render()

        if (StringUtil.isBlank(dot)) throw Exception("cannot build NFA")
        genFsa.generateFile(filename, dot)
        return filename
    }

    private fun prepare() {
        renderer
            .appendLine("digraph G {")
            .tab().appendLine("graph[splines=true,rankdir=LR]")
            .tab().appendLine("node [shape=square,style=rounded]")
            .tab().appendLine("edge [penwidth=0.5]")
    }

    private fun finish() {
        renderer.tab().appendLine("$HIDDEN_INIT [shape=none,label=\"\"]")
        renderer.tab().appendLine(String.format("%s -> %s", HIDDEN_INIT, graph.initId))
        renderer.appendLine("}")
    }

    private fun processItem(state: ModalSimState) {

        // state
        renderNodeForState(state)

        // steps
        renderer.appendLine("")
        for ((action, bes) in state.steps) {
            val actionNodeId = "${state.id}_${action.name}"
            renderNode(actionNodeId, "∧")
            renderBes(actionNodeId, bes)
            renderStateToAction(state.id, action, actionNodeId)
        }
    }

    private fun renderBes(nodeId: String,  bes: SimBesBase) {
        if (bes is ModalSimState) {
            renderEdge(nodeId, bes.id)
            return
        }

        if (bes is FalseSimBes) {
            renderErrorNode(nodeId)
            return
        }

        if (bes is CollectionSimBes) {

            val label = when (bes) {
                is AndSimBes -> "∧"
                else -> "∨"
            }
            for (index in bes.items.indices) {
                val dstId = "${nodeId}_$index"
                renderNode(dstId, label)
                renderEdge(nodeId, dstId)

                renderBes(dstId, bes.items[index])
            }
        }
    }

    private fun renderNode(nodeId: String, label: String) {
        renderer.tab().appendLine("$nodeId [label=$label,shape=circle,color=gray]")
    }

    private fun renderErrorNode(nodeId: String) {
        renderer.tab().appendLine("$nodeId [label=__,color=$ColorError,style=dashed]")
    }

    private fun renderNodeForState(state: ModalSimState): String {
        // get color
        val id = state.id
        val color =
            if (state.cachedErrorActions.isNotEmpty()) ColorError else if (state.isIncomplete) "rosybrown" else "black"
        val suffix = if (state.isIncomplete) ",style=\"dashed,rounded\"" else ""
        val st1 = state.stSpec?.name ?: "_"
        val st2 = state.stImpl?.name ?: "_"
        renderer.tab()
            .appendLine("$id [label=<<font color='$ColorSpec'>$st1</font><br/><font color='$ColorImpl'>$st2</font>>,color=$color$suffix]")
        return id
    }

    private fun renderStateToAction(srcId: String, action: ModalSimAction, dstId: String) {
        val text = String.format(
            "%s -> %s [label=<<font color='$ColorSpec'>%s</font>/<font color='$ColorImpl'>%s</font>>,color=%s,style=%s]",
            srcId,
            dstId,
            action.actionSpec.formattedString(),
            action.actionImpl.formattedString(),
            action.getNfaColor(),
            if (action.isMayTran) "dashed" else "solid"
        )
        renderer.tab().appendLine(text)
    }

    private fun renderEdge(srcId: String, dstId: String) {
        renderer.tab().appendLine("$srcId -> $dstId")
    }

    companion object {
        private const val HIDDEN_INIT = "_qi"

        private const val ColorError = ColorUtil.NfaColorError

        private const val ColorSpec = ColorUtil.NfaColorSpec

        private const val ColorImpl = ColorUtil.NfaColorImpl
    }
}