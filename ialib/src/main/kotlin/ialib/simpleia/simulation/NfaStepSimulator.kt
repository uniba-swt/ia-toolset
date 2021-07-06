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

import ialib.core.simulation.SimAction
import ialib.core.simulation.SimActionType
import ialib.core.simulation.RefinementType
import ialib.core.simulation.SubSimAction
import ialib.core.simulation.SubSimAction.Companion.create
import ialib.core.simulation.SubSimAction.Companion.createEpsilon
import ialib.core.simulation.SubSimAction.Companion.createError

class NfaStepSimulator {

    private val closureSetProvider: ClosureSetProvider = ClosureSetProvider()

    fun updateStepsToState(state: SimState) {
        if (state.stSpec == null || state.stImpl == null)
            return

        val stSpec = state.stSpec
        val stImpl = state.stImpl

        // list all input actions by the Spec
        for (action in stSpec.inputActions) {
            var dst: SimState
            val fstSubSimAction = create(action.name, SimActionType.Input)
            var sndSubSimAction: SubSimAction
            if (stImpl.hasAction(action)) {
                dst = SimState(stSpec.getInputDstState(action), stImpl.getInputDstState(action))
                sndSubSimAction = create(action.name, SimActionType.Input)
            } else {
                dst = SimState(stSpec.getInputDstState(action), null)
                sndSubSimAction = createError()
            }
            val simAct = SimAction(
                    fstSubSimAction,
                    sndSubSimAction,
                    RefinementType.Spec)
            state.addStep(simAct, dst)
        }

        // go through output and internal actions of Impl
        val closureSet = closureSetProvider.getOrComputeEClosureSet(stSpec)

        // add action for internal from impl
        for (internalAct in stImpl.internalActions) {
            val simAct = SimAction(
                    createEpsilon(),
                    create(internalAct.name, SimActionType.Internal),
                    RefinementType.Impl)
            for (closureSt in closureSet) {
                for (dstTauState in stImpl.getDstStates(internalAct)) {
                    val dst = SimState(closureSt, dstTauState)
                    state.addStep(simAct, dst)
                }
            }
        }

        // add action for output from impl
        for (outAct in stImpl.outputActions) {
            val simAct = SimAction(
                    create(outAct.name, SimActionType.Output),
                    create(outAct.name, SimActionType.Output),
                    RefinementType.Impl
            )
            val setDstImplSt = stImpl.getDstStates(outAct)
            var isValidOutput = false
            for (closureSt in closureSet) {
                if (closureSt.hasAction(outAct)) {
                    isValidOutput = true
                    for (dstClosureSt in closureSt.getDstStates(outAct)) {
                        for (dstImplSt in setDstImplSt) {
                            val dst = SimState(dstClosureSt, dstImplSt)
                            state.addStep(simAct, dst)
                        }
                    }
                }
            }

            // check if having no output
            if (!isValidOutput) {
                val errState = SimState(null, setDstImplSt.iterator().next())
                val act = SimAction(
                        createError(),
                        create(outAct.name, SimActionType.Output),
                        RefinementType.Impl
                )
                state.addStep(act, errState)
            }
        }
    }

}