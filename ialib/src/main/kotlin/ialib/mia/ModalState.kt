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

package ialib.mia

import ialib.core.AbstractState
import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import ialib.iam.expr.MActionExpr
import java.util.*

class ModalState(name: String, isInitial: Boolean) : AbstractState(name, isInitial) {

    val mustInputActions: MutableSet<AutomatonAction> = TreeSet()

    val mustOutputActions: MutableSet<AutomatonAction> = TreeSet()

    val mustInternalActions: MutableSet<AutomatonAction> = TreeSet()

    /**
     * destination of must step is a disjunctive set of states
     */
    private val mustSteps: MutableMap<AutomatonAction, MutableList<ModalStep>> = mutableMapOf()

    private val maySteps: MutableMap<AutomatonAction, MutableList<ModalStep>> = mutableMapOf()

    val mayInputActions: MutableSet<AutomatonAction> = TreeSet()

    val mayOutputActions: MutableSet<AutomatonAction> = TreeSet()

    val mayInternalActions: MutableSet<AutomatonAction> = TreeSet()

    fun addMustTransition(action: MActionExpr, dsts: List<ModalState>) {
        initTransitionIfNeed(mustSteps, action, false).add(ModalStep(action, dsts))
    }

    fun addMayTransition(action: MActionExpr, dst: ModalState) {
        initTransitionIfNeed(maySteps, action, true).add(ModalStep(action, dst))
    }

    private fun initTransitionIfNeed(
        steps: MutableMap<AutomatonAction, MutableList<ModalStep>>,
        action: MActionExpr,
        isMayTran: Boolean
    ): MutableList<ModalStep> {
        val inner = action.action
        if (!steps.containsKey(inner)) {
            steps[inner] = mutableListOf()
            addAction(inner, isMayTran)
        }

        return steps[inner]!!
    }

    private fun addAction(action: AutomatonAction, isMayTran: Boolean) {
        // add to the front
        val set = if (isMayTran) {
            when (action.actionType) {
                AutomatonActionType.Input -> mayInputActions
                AutomatonActionType.Output -> mayOutputActions
                AutomatonActionType.Internal -> mayInternalActions
            }
        } else {
            when (action.actionType) {
                AutomatonActionType.Input -> mustInputActions
                AutomatonActionType.Output -> mustOutputActions
                AutomatonActionType.Internal -> mustInternalActions
            }
        }
        set.add(action)

        // duplicated the collection
        addActionInternally(action)
    }

    fun getMaySteps(action: AutomatonAction): List<ModalStep> {
        return maySteps[action] ?: emptyList()
    }

    fun getMustSteps(action: AutomatonAction): List<ModalStep> {
        return mustSteps[action] ?: emptyList()
    }

    fun getAllMaySteps(): Collection<List<ModalStep>> {
        return maySteps.values
    }

    fun getAllMustSteps(): Collection<List<ModalStep>> {
        return mustSteps.values
    }

    fun getStepsSequence(action: AutomatonAction): Sequence<ModalStep> {
        return mustSteps.getOrDefault(action, mutableListOf()).asSequence() + maySteps.getOrDefault(action, mutableListOf()).asSequence()
    }
}
