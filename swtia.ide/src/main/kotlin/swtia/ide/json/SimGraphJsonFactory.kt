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

package swtia.ide.json

import ialib.core.simulation.RefinementType
import ialib.core.simulation.SimAction
import ialib.iam.MemStep
import swtia.ide.json.IamJsonModel
import ialib.iam.simulation.SimActionResult
import ialib.iam.simulation.SimGraph
import ialib.iam.simulation.SimMemState

object SimGraphJsonFactory {

    /**
     * convert graph to json obj
     */
    fun SimGraph.toSimGraphJson() : SimGraphJson {
        return SimGraphJson(
            IamJsonModel.from(this.interAbstract.name, this.interAbstract),
            IamJsonModel.from(this.interSpecific.name, this.interSpecific),
            this.states.map { s -> s.toSimStateJson() },
            this.initId
        )
    }

    /**
     * convert mem state to json obj
     */
    private fun SimMemState.toSimStateJson(): SimMemStateJson {
        return SimMemStateJson(
            this.id,
            this.isIncomplete,
            this.actionResults.map { r -> r.toSimMemStateOptionJson(this) }.flatten()
        )
    }

    private fun SimActionResult.toSimMemStateOptionJson(simMemState: SimMemState): List<SimMemStateOptionJson> {

        val defenceEdgeType = simAction.edgeType.reverse()

        // check error states
        if (this.stateSteps.isNotEmpty()) {

            // error: no action found
            if (this.simAction.isError) {
                // the pre and post-condition is ignored
                val attack = SimOptionAttackJson(
                    simAction.edgeType,
                    getFirstStep(simMemState, simAction).id)

                val defence = SimOptionDefenceJson(
                    defenceEdgeType,
                    DefenceStrategyType.ErrorNoAction,
                    emptyList())
                return listOf(SimMemStateOptionJson(attack, defence))
            }
        }

        // check families
        if (this.familySteps.isNotEmpty()) {
            for (familyStep in this.familySteps) {

                // error: no family found
                if (familyStep.errorNoFamilies) {
                    val attack = SimOptionAttackJson(
                        familyStep.refinementType,
                        familyStep.step.id)

                    val defence = SimOptionDefenceJson(
                        defenceEdgeType,
                        DefenceStrategyType.ErrorNoFamily,
                        emptyList()
                    )
                    return listOf(SimMemStateOptionJson(attack, defence))
                }
            }
        }

        // 2nd phase: show selection

        // check immediate states
        if (this.stateSteps.isNotEmpty()) {
            val attack = SimOptionAttackJson(
                simAction.edgeType,
                getFirstStep(simMemState, simAction).id)

            // now defence has a set of transition
            val defenceSteps = getSteps(simMemState, defenceEdgeType, simAction)

            // each step forms a family already (1 member)
            val families = defenceSteps
                .map { s -> SimOptionDefenceFamilyJson(listOf(SimOptionDefenceFamilyMemberJson(s.id, this.stateSteps.first().id))) }

            val defence = SimOptionDefenceJson(
                defenceEdgeType,
                DefenceStrategyType.Families,
                families
            )

            return listOf(SimMemStateOptionJson(attack, defence))
        }

        // check family
        if (this.familySteps.isNotEmpty()) {
            return this.familySteps.map { familyStep ->
                val attack = SimOptionAttackJson(
                    simAction.edgeType,
                    familyStep.step.id)

                // update order
                val defenceFamilies = familyStep.families
                    .map { f ->
                        f.refinedSteps.map { s -> SimOptionDefenceFamilyMemberJson(s.step.id, s.dstSimState.id) }
                    }
                    .map { members -> SimOptionDefenceFamilyJson(members) }

                val defence = SimOptionDefenceJson(
                    defenceEdgeType,
                    DefenceStrategyType.Families,
                    defenceFamilies
                )

                SimMemStateOptionJson(attack, defence)
            }
        }

        // empty state
        error("invalid model, at lease stateSteps or familySteps must be present")
    }

    private fun getFirstStep(simMemState: SimMemState, simAction: SimAction): MemStep {
        return getSteps(simMemState, simAction.edgeType, simAction).first()
    }

    private fun getSteps(simMemState: SimMemState, edgeType: RefinementType, simAction: SimAction): List<MemStep> {
        // if lead by spec
        if (edgeType.isSpec()) {
            return simMemState.stSpec!!.getDstSteps(simAction.actionSpec.toAutomatonAction())
        }

        // if lead of impl
        return simMemState.stImpl!!.getDstSteps(simAction.actionImpl.toAutomatonAction())
    }
}