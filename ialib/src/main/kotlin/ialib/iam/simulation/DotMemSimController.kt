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

package ialib.iam.simulation

import com.google.common.escape.Escaper
import com.google.common.html.HtmlEscapers
import ialib.core.services.TextFileWriter
import ialib.core.simulation.SimAction
import ialib.core.simulation.RefinementType
import ialib.dot.StringRenderer
import ialib.util.ColorUtil
import ialib.util.StringUtil

class DotMemSimController(private val genFsa: TextFileWriter, private val graph: SimGraph) {

    private val renderer: StringRenderer = StringRenderer()
    private val escaper: Escaper = HtmlEscapers.htmlEscaper()

    private val filename = "IAM_Simulation_${graph.name}.dot"

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

    private fun processItem(item: SimMemState) {
        // state
        renderNodeForState(item)

        // actions and steps
        for (result in item.actionResults) {
            val action = result.simAction

            // append
            renderer.tab().appendLine("")

            // check state
            if (result.stateSteps.isNotEmpty()) {
                result.stateSteps.forEach { dst ->
                    renderStateToState(item, dst, action)
                }
            }

            // check familySteps
            if (result.familySteps.isNotEmpty()) {
                // first go to an action point
                // example: r1_p1 -a?/a? -> r1_p1_a
                val actionNodeId = renderStateToAction(item, action)

                // from action point to child steps
                for ((stepIdx, familyStep) in result.familySteps.withIndex()) {

                    // to the point for family
                    val stepId = "${actionNodeId}_s$stepIdx"
                    val isErrorStep = familyStep.errorNoFamilies
                    renderStepNode(stepId, action.edgeType, isErrorStep)
                    renderActionToStep(actionNodeId, familyStep, stepId)

                    if (!isErrorStep) {
                        // families
                        for ((familyIdx, family) in familyStep.families.withIndex()) {
                            val familyId = renderStepToFamily(stepId, family, familyIdx)

                            // each member in family
                            for (refinedStep in family.refinedSteps) {
                                // create new state
                                renderFamilyEdge(familyId, family, refinedStep)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun prepare() {
        renderer
            .appendLine("digraph G {")
            .tab().appendLine("graph[splines=true,rankdir=LR]")
            .tab().appendLine("node [shape=point,style=rounded]")
            .tab().appendLine("edge [penwidth=0.5]")
    }

    private fun finish() {
        renderer.tab().appendLine("$HIDDEN_INIT [shape=none,label=\"\"]")
        renderer.tab().appendLine(String.format("%s -> %s", HIDDEN_INIT, graph.initId))
        renderer.appendLine("}")
    }

    /**
     * state -> state
     */
    private fun renderStateToState(state: SimMemState, dst: SimMemState, action: SimAction) {
        renderer.tab().appendLine("${state.id} -> ${dst.id} ${getActionLabel(action)}")
    }

    /**
     * node for state
     * r1_p1
     */
    private fun renderNodeForState(state: SimMemState) {
        val color = if (state.isIncomplete) "rosybrown" else "black"
        val suffix = if (state.isIncomplete) ",style=\"dashed,rounded\"" else ""
        renderer.tab().appendLine("${state.id} [shape=square,label=<<font color='blue3'>${state.stSpec?.name ?: "_"}</font><br/><font color='darkgreen'>${state.stImpl?.name ?: "_"}</font>>,color=$color$suffix]")
    }

    /**
     * state to action
     * r1_p1 - a? / a? -> r1_p1_a
     */
    private fun renderStateToAction(state: SimMemState, action: SimAction): String {
        val id = "${state.id}_${action.name}"
        renderer.tab().appendLine("$id [shape=point]")
        renderer.tab().appendLine("${state.id} -> $id ${getActionLabel(action)}")
        return id
    }

    private fun getActionLabel(action: SimAction) : String {
        val color = getActionColor(action)
        return "[label=<<font color='$ColorSpec'>${action.actionSpec.formattedString()}</font><br/><font color='$ColorImpl'>${action.actionImpl.formattedString()}</font>>,color=$color,arrowhead=none]"
    }

    /**
     * r1_p1_a_s0 [shape=point, label=""]
     */
    private fun renderStepNode(stepId: String, edgeType: RefinementType, isError: Boolean) {
        val color = if (isError) "red" else getNormalEdgeType(edgeType)
        renderer.tab().appendLine("$stepId [shape=point,label=\"\",color=$color]")
    }

    /**
     * action to spec:
     * r1_p1_a - [x == 0] [x >= 0] -> r1_p1_a_s0
     */
    private fun renderActionToStep(actionNodeId: String, step: FamiliesSimStep, stepId: String) {
        val color = getStepColor(step.refinementType)
        renderer.tab().appendLine("$actionNodeId -> $stepId [label=<<font color='$color'>${escaper.escape(step.step.toString())}</font>>,color=$color,arrowhead=none]")
    }

    /**
     * step to family
     * r1_p1_a - [x == 0] [x >= 0] -> r1_p1_a_s0_f0
     */
    private fun renderStepToFamily(stepId: String, family: RefinementFamily, index: Int): String {
        val id = "${stepId}_f$index"
        val color = getStepColor(family.refinementType)
        renderer.tab().appendLine("$stepId -> $id [color=$color,arrowhead=none]")
        return id
    }

    /**
     * family to next comp state
     *
     * r1_p1_a_s0_f0 - [x == 0] [x < 1] -> r2_p2
     */
    private fun renderFamilyEdge(familyId: String, family: RefinementFamily, refinedStep: RefinementStep) {
        val dstStateId = refinedStep.dstSimState.id
        val color = getStepColor(family.refinementType)
        renderer.tab().appendLine("$familyId -> $dstStateId [label=<<font color='$color'>${escaper.escape(refinedStep.step.toString())}</font>>,color=$color]")
    }

    private fun getActionColor(action: SimAction): String {
        return if (action.isError) ColorError else getNormalEdgeType(action.edgeType)
    }

    private fun getNormalEdgeType(edgeType: RefinementType): String {
        return if (edgeType === RefinementType.Spec) ColorSpec else ColorImpl
    }

    private fun getStepColor(type: RefinementType): String = if (type == RefinementType.Spec) ColorSpec else ColorImpl

    companion object {
        private const val HIDDEN_INIT = "_qi"

        private const val ColorError = ColorUtil.NfaColorError

        private const val ColorSpec = ColorUtil.NfaColorSpec

        private const val ColorImpl = ColorUtil.NfaColorImpl
    }
}