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

import ialib.core.AbstractState
import ialib.core.simulation.AbstractSimState
import ialib.core.simulation.SimAction
import ialib.iam.MemState

class SimMemState(val stSpec: MemState?, val stImpl: MemState?) : AbstractSimState() {

    val actionResults = mutableListOf<SimActionResult>()

    private val mapActions = mutableMapOf<SimAction, SimActionResult>()

    fun addFamilyStep(action: SimAction, simStep: FamiliesSimStep) {
        val item = createActionResultIfNeeded(action)
        item.familySteps.add(simStep)
    }

    private fun createActionResultIfNeeded(action: SimAction): SimActionResult {
        if (!mapActions.containsKey(action)) {
            val item = SimActionResult(action)
            actionResults.add(item)
            mapActions[action] = item
            return item
        }
        return mapActions[action]!!
    }

    fun addStateStep(action: SimAction, dst: SimMemState) {
        val item = createActionResultIfNeeded(action)
        item.stateSteps.add(dst)
    }

    override val baseSpecState: AbstractState?
        get() = stSpec

    override val baseImplState: AbstractState?
        get() = stImpl

    val specId: String
        get() = stSpec?.name ?: ""

    val implId: String
        get() = stImpl?.name ?: ""
}

class SimActionResult(val simAction: SimAction) {
    val familySteps = mutableListOf<FamiliesSimStep>()

    /**
     * if the sim action is internal -> immediate states
     * if the sim action is input or output -> set of error states
     */
    val stateSteps = mutableListOf<SimMemState>()
}