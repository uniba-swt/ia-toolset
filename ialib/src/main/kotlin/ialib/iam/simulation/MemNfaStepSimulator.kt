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

import ialib.core.AutomatonAction
import ialib.core.simulation.SimAction
import ialib.core.simulation.SimActionType
import ialib.core.simulation.RefinementType
import ialib.core.simulation.SubSimAction
import ialib.iam.MemState
import ialib.iam.MemStep
import ialib.iam.expr.solver.DefaultSmtSolver

class MemNfaStepSimulator(private val solver: DefaultSmtSolver) {

    private val closureSetProvider: MemClosureSetProvider = MemClosureSetProvider()

    fun updateStepsToState(state: SimMemState) {
        if (state.stSpec == null || state.stImpl == null)
            return

        val stSpec: MemState = state.stSpec
        val stImpl: MemState = state.stImpl

        // input by spec
        for (inAction in stSpec.inputActions) {
            processInputAction(state, stSpec, stImpl, inAction)
        }

        // internal by impl
        val closureSet: Set<MemState> = closureSetProvider.getOrComputeEClosureSet(stSpec)
        for (internalAct in stImpl.internalActions) {
            processInternalAction(state, closureSet, stImpl, internalAct)
        }

        // output by impl
        for (outAction in stImpl.outputActions) {
            processOutputAction(state, closureSet, stImpl, outAction)
        }
    }

    private fun processInputAction(state: SimMemState, stSpec: MemState, stImpl: MemState, inAction: AutomatonAction) {
        val fstSub = SubSimAction.create(inAction.name, SimActionType.Input)
        val specSteps = stSpec.getDstSteps(inAction)
        if (stImpl.hasAction(inAction)) {

            // go through each step
            val simAction = SimAction(fstSub, SubSimAction.create(inAction.name, SimActionType.Input), RefinementType.Spec)
            for (specStep in specSteps) {
                val familyStep = processFamilyStep(true, specStep, stImpl.getDstSteps(inAction))
                state.addFamilyStep(simAction, familyStep)
            }
        } else {
            // error action
            val simAction = SimAction(fstSub, SubSimAction.createError(), RefinementType.Spec)
            val errState = SimMemState(specSteps.iterator().next().dstState, null)
            state.addStateStep(simAction, errState)
        }
    }

    private fun processInternalAction(state: SimMemState, closureSet: Set<MemState>, stImpl: MemState, internalAct: AutomatonAction) {
        val simAct = SimAction(
                SubSimAction.createEpsilon(),
                SubSimAction.create(internalAct.name, SimActionType.Internal),
                RefinementType.Impl)

        for (specClosureState in closureSet) {
            for (implTauStep in stImpl.getDstSteps(internalAct)) {
                val dstState = SimMemState(specClosureState, implTauStep.dstState)
                state.addStateStep(simAct, dstState)
            }
        }
    }

    private fun processOutputAction(state: SimMemState, closureSet: Set<MemState>, stImpl: MemState, outAction: AutomatonAction) {
        val sndSub = SubSimAction.create(outAction.name, SimActionType.Output)
        val implSteps = stImpl.getDstSteps(outAction)
        val simAction = SimAction(SubSimAction.create(outAction.name, SimActionType.Output), sndSub, RefinementType.Impl)

        // go through closure set for organizing the dst states
        val closureSteps = closureSet.flatMap { closureSt -> closureSt.getDstSteps(outAction) }
        if (closureSteps.isEmpty()) {
            // error action
            state.addStateStep(
                    SimAction(SubSimAction.createError(), sndSub, RefinementType.Impl),
                    SimMemState(null, implSteps.iterator().next().dstState)
            )
        } else {
            // go through each step
            for (implStep in implSteps) {
                val familyStep = processFamilyStep(false, implStep, closureSteps)
                state.addFamilyStep(simAction, familyStep)
            }
        }
    }

    private fun processFamilyStep(checkingInput: Boolean, specStep: MemStep, implSteps: List<MemStep>): FamiliesSimStep {
        val familiesType = if (checkingInput) RefinementType.Spec else RefinementType.Impl
        val provider = RefinementFamilyProvider( solver, specStep, familiesType, checkingInput, specStep, implSteps)
        val families: List<RefinementFamily> = provider.exec()
        return FamiliesSimStep(specStep, familiesType, families)
    }
}