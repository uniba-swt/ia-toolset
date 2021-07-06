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

package ialib.iam.refinement

import ialib.bes.*
import ialib.core.simulation.SimActionType
import ialib.core.simulation.RefinementType
import ialib.iam.simulation.FamiliesSimStep
import ialib.iam.simulation.SimGraph
import ialib.iam.simulation.SimMemState
import org.apache.log4j.Logger
import java.util.*

class MemBesFormulaBuilder(private val graph: SimGraph): BesFormulaBuilderInterface {

    private val nuItems = TreeSet<NuFormulaItem>()
    private var init: NuFormulaItem? = null

    override fun build(): DocBesFormula? {
        for (state in graph.states) {
            processItem(state)
        }
        return init?.let { DocBesFormula(it, nuItems) }
    }

    private fun processItem(item: SimMemState) {
        // build formula for each state
        val frm: NuFormulaItem = buildNuFormulaForState(item)
        if (item.isInit)
            init = frm

        nuItems.add(frm)
    }

    private fun buildNuFormulaForState(state: SimMemState): NuFormulaItem {
        logger.debug("build NU formula for state: " + state.id)

        // incomplete
        if (state.isIncomplete) {
            return NuFormulaItem(state.id, AtomBesFormula.createFalse())
        }

        // create AND of actions
        val actionForms: MutableList<BesFormulaBase> = mutableListOf()
        for (result in state.actionResults) {

            // create OR of direct states
            if (result.stateSteps.isNotEmpty()) {
                actionForms.add(BesFormulaFactory.createOr(result.stateSteps.map { s -> AtomBesFormula.createVar(s.id) }))
            }

            // create AND of family steps
            if (result.familySteps.isNotEmpty()) {
                val stepForms: MutableList<BesFormulaBase> = mutableListOf()
                for (familyStep: FamiliesSimStep in result.familySteps) {

                    // empty families -> FALSE
                    if (familyStep.errorNoFamilies) {
                        stepForms.add(AtomBesFormula.createFalse())
                    } else {
                        // create OR of family
                        val familyForms: MutableList<BesFormulaBase> = mutableListOf()
                        for (family in familyStep.families) {

                            // create AND/OR of family step
                            val familyStepForms: MutableList<BesFormulaBase> = mutableListOf()
                            for (refinedStep in family.refinedSteps) {
                                // create new state
                                familyStepForms.add(AtomBesFormula.createVar(refinedStep.dstSimState.id))
                            }

                            // find suitable op type: AND for family members
                            familyForms.add(BesFormulaFactory.createAnd(familyStepForms))
                        }
                        stepForms.add(BesFormulaFactory.createOr(familyForms))
                    }
                }
                actionForms.add(BesFormulaFactory.createAnd(stepForms))
            }
        }

        return NuFormulaItem(state.id, BesFormulaFactory.createAnd(actionForms))
    }

    companion object {
        private val logger = Logger.getLogger(MemBesFormulaBuilder::class.java)
    }
}