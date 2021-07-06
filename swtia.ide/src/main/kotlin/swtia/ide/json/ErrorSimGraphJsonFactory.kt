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
import ialib.iam.json.sim.ErrorFamilyFinder
import ialib.iam.simulation.SimActionResult
import ialib.iam.simulation.SimGraph
import ialib.iam.simulation.SimMemState

object ErrorSimGraphJsonFactory {

    /**
     * convert graph to json obj
     */
    fun toSimGraphJson(gr: SimGraph) : SimGraphJson {
        val finder = ErrorFamilyFinder(gr)
        return SimGraphJson(
            IamJsonModel.from(gr.interAbstract.name, gr.interAbstract),
            IamJsonModel.from(gr.interSpecific.name, gr.interSpecific),
            gr.states.map { s -> s.toSimStateJson(finder) },
            gr.initId
        )
    }

    /**
     * convert mem state to json obj
     */
    private fun SimMemState.toSimStateJson(finder: ErrorFamilyFinder): SimMemStateJson {

        val pairs: List<Pair<SimMemStateOptionJson, Boolean>> = this.actionResults.map { r -> r.toSimMemStateOptionJson(this, finder) }

        // if having error, remove all others
        val items: List<SimMemStateOptionJson> = when {
            pairs.any { p -> p.second } -> pairs.filter { p -> p.second }.map { p -> p.first }
            else -> pairs.map { p -> p.first }
        }

        return SimMemStateJson(this.id, finder.checkIfErrorState(this), items)
    }

    private fun SimActionResult.toSimMemStateOptionJson(simMemState: SimMemState, finder: ErrorFamilyFinder): Pair<SimMemStateOptionJson, Boolean> {

        val defenceEdgeType = simAction.edgeType.reverse()

        // 1st phase: find error

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
                return Pair(SimMemStateOptionJson(attack, defence), true)
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
                    return Pair(SimMemStateOptionJson(attack, defence), true)
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

            return Pair(SimMemStateOptionJson(attack, defence), false)
        }

        // check family
        if (this.familySteps.isNotEmpty()) {
            // attack the first one
            val familyStep = finder.findErrorFamily(simMemState, this.familySteps)
            val attack = SimOptionAttackJson(
                simAction.edgeType,
                familyStep.step.id)

            // update order
            val defenceFamilies = familyStep.families
                .map { f ->
                    val orderedRefined = finder.sortRefinedSteps(f.refinedSteps)
                    orderedRefined.map { s -> SimOptionDefenceFamilyMemberJson(s.step.id, s.dstSimState.id) }
                }
                .map { members -> SimOptionDefenceFamilyJson(members) }

            val defence = SimOptionDefenceJson(
                defenceEdgeType,
                DefenceStrategyType.Families,
                defenceFamilies
            )

            return Pair(SimMemStateOptionJson(attack, defence), false)
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