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

package ialib.simpleia.simulation

import ialib.Ulogger
import ialib.core.simulation.SimAction
import ialib.simpleia.Automaton
import ialib.dot.StringRenderer
import ialib.util.ColorUtil
import java.util.function.Consumer

class NfaSimBuilder(specIa: Automaton, implIa: Automaton) : AbstractIASimTraversor(specIa, implIa) {
    private val renderer = StringRenderer()
    private var initId: String? = null
    fun build(): String? {
        Ulogger.debug("start building NFA")

        // prepare
        renderer.reset()
        prepare()

        // result
        if (!start()) return null

        // finish
        finish()
        return renderer.render()
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
        renderer.tab().appendLine(String.format("%s -> %s", HIDDEN_INIT, initId))
        renderer.appendLine("}")
    }

    override fun processSimState(state: SimState, queueProvider: Consumer<SimState>) {
        super.processSimState(state, queueProvider)
        if (state.isInit) initId = state.id

        // state
        renderNodeForState(state)

        // steps
        renderer.appendLine("")
        for (action in state.actions) {
            for (dstState in state.getDstStates(action)) {
                // add to queue
                queueProvider.accept(dstState)

                // process
                renderStep(state, action, dstState)
            }
        }
    }

    private fun renderStep(state: SimState, action: SimAction, dstState: SimState) {
        val text = String.format("%s -> %s [label=<<font color='${ColorUtil.NfaColorSpec}'>%s</font>/<font color='${ColorUtil.NfaColorImpl}'>%s</font>>,color=%s]",
            state.id,
            dstState.id,
            action.actionSpec.formattedString(),
            action.actionImpl.formattedString(),
            action.getNfaColor())
        renderer.tab().appendLine(text)
    }

    private fun renderNodeForState(state: SimState) {
        // get color
        val color = if (state.isHasErrorAction) ColorUtil.NfaColorError else if (state.isIncomplete) "rosybrown" else "black"
        val suffix = if (state.isIncomplete) ",style=\"dashed,rounded\"" else ""
        val st1 = state.stSpec?.name ?: ""
        val st2 = state.stImpl?.name ?: ""
        val text = String.format("%s [label=<<font color='${ColorUtil.NfaColorSpec}'>%s</font><br/><font color='${ColorUtil.NfaColorImpl}'>%s</font>>,color=%s%s]",
            state.id,
            st1,
            st2,
            color,
            suffix)
        renderer.tab().appendLine(text)
    }

    companion object {
        private const val HIDDEN_INIT = "_qi"
    }
}
