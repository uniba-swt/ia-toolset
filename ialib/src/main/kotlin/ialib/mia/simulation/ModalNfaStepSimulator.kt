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

import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import ialib.mia.ModalStep
import ialib.mia.ModalState
import ialib.mia.weak.WeakAction
import ialib.mia.weak.WeakTransitionWrapper
import ialib.core.simulation.*
import ialib.mia.weak.ModalStateCol

class ModalNfaStepSimulator {

    private val weakProvider = WeakTransitionWrapper()

    fun updateStepsToState(state: ModalSimState) {
        if (state.stSpec == null || state.stImpl == null)
            return

        val spec: ModalState = state.stSpec
        val impl: ModalState = state.stImpl

        updateMustTransitions(state, spec, impl)
        updateMayTransitions(state, spec, impl)
    }

    /**
     * handle must transitions
     */
    private fun updateMustTransitions(state: ModalSimState, spec: ModalState, impl: ModalState) {
        // must transitions - inputs
        val mustActionsSequence: Sequence<AutomatonAction> =
            spec.mustInputActions.asSequence() + spec.mustOutputActions.asSequence() + spec.mustInternalActions.asSequence()
        for (action in mustActionsSequence) {
            val simAction = ModalSimAction(action, RefinementType.Spec, false)

            // find impl dsts
            val weakAction = when (action.actionType) {
                AutomatonActionType.Input -> WeakAction.trailingInput(action.name)
                AutomatonActionType.Output -> WeakAction.outputAction(action.name)
                AutomatonActionType.Internal -> WeakAction.epsilon()
            }
            val implTrailingDests = weakProvider.getWeakMustTrans(impl, weakAction)

            // go through each spec set
            if (implTrailingDests.isNotEmpty()) {
                val specModalDests = spec.getMustSteps(action)
                val bes = SimBesFactory.and(specModalDests.map { dest -> buildMustBes(dest, implTrailingDests) })
                state.addAction(simAction, bes)
            } else {
                state.addAction(ModalSimAction(action.toSubSimAction(), SubSimAction.createError(), RefinementType.Spec, true), FalseSimBes())
            }
        }
    }

    /**
     * handle may transitions
     */
    private fun updateMayTransitions(state: ModalSimState, spec: ModalState, impl: ModalState) {

        // go through all actions in implementation (must + may)
        for (action in impl.actionsSequence) {

            val simAction = ModalSimAction(action, RefinementType.Impl, true)
            val implModalDestSeq = impl.getStepsSequence(action)

            // find all destination of may transition (incl. must transition)
            val weakAction = when (action.actionType) {
                AutomatonActionType.Input -> WeakAction.trailingInput(action.name)
                AutomatonActionType.Output -> WeakAction.outputAction(action.name)
                AutomatonActionType.Internal -> WeakAction.epsilon()
            }
            val specDsts = weakProvider.getWeakMayTrans(spec, weakAction)

            // find corresponding implementation
            // go through each spec set
            if (specDsts.isNotEmpty()) {
                val implStates = implModalDestSeq.map { s -> s.states }.flatten()
                val bes = SimBesFactory.and(implStates.map { dest -> buildMayBes(specDsts, dest) }.toList())
                state.addAction(simAction, bes)
            } else {
                state.addAction(ModalSimAction(SubSimAction.createError(), action.toSubSimAction(), RefinementType.Impl, true), FalseSimBes())
            }
        }
    }

    private fun buildMustBes(specStep: ModalStep, implSteps: List<ModalStateCol>): SimBesBase {
        val specDsts = specStep.states
        // build OR
        return SimBesFactory.or(implSteps.map { implDest ->
            // build AND
            SimBesFactory.and(implDest.states.map { impl ->
                // build OR
                SimBesFactory.or(specDsts.map { spec ->
                    // build destination sim modal state
                    ModalSimState(
                        spec,
                        impl
                    )
                })
            })
        })
    }

    private fun buildMayBes(specDsts: List<ModalState>, impl: ModalState): SimBesBase {
        // build OR
        return SimBesFactory.or(specDsts.map { spec ->
            ModalSimState(
                spec,
                impl
            )
        })
    }
}
