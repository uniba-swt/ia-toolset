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

import ialib.Ulogger

class ErrorFamilyFinder(private val ia: SimGraph) {
    private val mapErrorStates = mutableSetOf<String>()

    init {
        indexErrorStates()
    }

    private fun indexErrorStates() {

        // index
        for (state in ia.states) {
            if (isErrorState(state)) {
                mapErrorStates.add(state.id)
            }
        }

        for (state in ia.states) {
            if (checkErrorStateMap(state)) {
                mapErrorStates.add(state.id)
            }
        }
        Ulogger.debug { "list refinement error states: $mapErrorStates" }
    }

    private fun checkErrorStateMap(state: SimMemState): Boolean {
        if (!mapErrorStates.contains(state.id)) {
            for (actionResult in state.actionResults) {
                for (stateStep in actionResult.stateSteps) {
                    if (mapErrorStates.contains(stateStep.id))
                        return true
                }

                for (familyStep in actionResult.familySteps) {
                    for (family in familyStep.families) {
                        for (refinedStep in family.refinedSteps) {
                            if (mapErrorStates.contains(refinedStep.dstSimState.id))
                                return true
                        }
                    }
                }
            }
        }

        return false
    }

    fun checkIfErrorState(state: SimMemState): Boolean {
        return mapErrorStates.contains(state.id)
    }

    private fun isErrorState(state: SimMemState): Boolean {
        // simply by error
        if (state.isIncomplete) {
            return true
        }

        // check if having no family
        if (state.actionResults.isNotEmpty()) {
            for (actionResult in state.actionResults) {
                // no action
                if (actionResult.simAction.isError) {
                    return true
                }

                // no family
                for (familyStep in actionResult.familySteps) {
                    if (familyStep.errorNoFamilies) {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun findErrorFamily(familySteps: MutableList<FamiliesSimStep>): FamiliesSimStep {
        // error first
        for (familyStep in familySteps) {
            if (familyStep.errorNoFamilies)
                return familyStep
        }

        // the one with error
        for (familyStep in familySteps) {
            for (family in familyStep.families) {
                if (family.refinedSteps.isEmpty())
                    return familyStep

                for (refinedStep in family.refinedSteps) {
                    if (mapErrorStates.contains(refinedStep.dstSimState.id))
                        return familyStep
                }
            }
        }

        return familySteps.first()
    }

    fun sortRefinedSteps(refinedSteps: List<RefinementStep>): List<RefinementStep> {
        return refinedSteps.sortedWith { o1, o2 ->
            val s1 = mapErrorStates.contains(o1.dstSimState.id)
            val s2 = mapErrorStates.contains(o2.dstSimState.id)
            s2.compareTo(s1)
        }
    }
}
