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

package ialib.simpleia.composition

import ialib.simpleia.Automaton
import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import ialib.simpleia.AutomatonBuilder
import ialib.core.product.ProductUtil.computeSharedIOActions
import ialib.core.product.ProductUtil.isComposable
import ialib.core.simulation.AbstractSimTraversor
import ialib.Ulogger
import ialib.core.CoreException
import java.util.function.Consumer

open class ProductOperation(private val ia1: Automaton, private val ia2: Automaton) : AbstractSimTraversor<CompositeState>() {
    private lateinit var builder: AutomatonBuilder
    private val errorStateIds: MutableSet<String> = mutableSetOf()
    private lateinit var sharedActNames: Set<String>

    protected open val namePrefix: String
        get() = "Product"

    /**
     * Build result automaton
     * @return automaton
     */
    fun build(): Automaton? {

        // composable
        if (!isParallelComposable) {
            throw CoreException("Automata are not composable: '${ia1.name}' and '${ia2.name}'")
        }

        val name = String.format("%s_%s_%s", namePrefix, ia1.name, ia2.name)
        builder = AutomatonBuilder(name)

        // shared action based on name (input/output action only)
        sharedActNames = computeSharedIOActions(
                ia1.inputActions,
                ia1.outputActions,
                ia2.inputActions,
                ia2.outputActions)

        // prepare
        start(CompositeState(ia1.initState, ia2.initState))

        // finish
        return builder.build()
    }

    override fun processSimState(state: CompositeState, queueProvider: Consumer<CompositeState>) {
        builder.addStateIfNeeded(state.cmpMemState)

        // cross/empty for output 1
        for (outputAction in state.st1.outputActions) {
            if (sharedActNames.contains(outputAction.name)) {
                if (!processSharedOutputAction(state, outputAction, queueProvider)) {
                    cacheErrorState(state.id)
                    return
                }
            } else {
                processEmptyAction(state, outputAction, true, queueProvider)
            }
        }

        // cross/empty for output 2
        for (outputAction in state.st2.outputActions) {
            if (sharedActNames.contains(outputAction.name)) {
                if (!processSharedOutputAction(state, outputAction, queueProvider)) {
                    cacheErrorState(state.id)
                    return
                }
            } else {
                processEmptyAction(state, outputAction, false, queueProvider)
            }
        }

        // empty for input 1
        for (inputAction in state.st1.inputActions) {
            if (!sharedActNames.contains(inputAction.name)) {
                processEmptyAction(state, inputAction, true, queueProvider)
            }
        }

        // empty for internal 1
        for (internalAction in state.st1.internalActions) {
            processEmptyAction(state, internalAction, true, queueProvider)
        }

        // empty for input 2
        for (inputAction in state.st2.inputActions) {
            if (!sharedActNames.contains(inputAction.name)) {
                processEmptyAction(state, inputAction, false, queueProvider)
            }
        }

        // empty for internal 2
        for (internalAction in state.st2.internalActions) {
            processEmptyAction(state, internalAction, false, queueProvider)
        }
    }

    private fun cacheErrorState(stateId: String) {
        errorStateIds.add(stateId)
    }

    private fun processEmptyAction(state: CompositeState, action: AutomatonAction, isFirst: Boolean, queueProvider: Consumer<CompositeState>) {
        // find owner
        val toMoveSt = if (isFirst) state.st1 else state.st2

        // add steps
        for (dstState in toMoveSt.getDstStates(action)) {
            // add step
            val prodDst = if (isFirst) CompositeState(dstState, state.st2) else CompositeState(state.st1, dstState)
            addNewStep(state, action, prodDst, queueProvider)
        }
    }

    private fun processSharedOutputAction(state: CompositeState, outputAction: AutomatonAction, queueProvider: Consumer<CompositeState>): Boolean {
        if (outputAction.actionType != AutomatonActionType.Output) return false

        // check if not exist
        val inputAction = AutomatonAction.ofInput(outputAction.name)
        if (state.st1.hasAction(outputAction) && !state.st2.hasAction(inputAction)) return false
        if (state.st2.hasAction(outputAction) && !state.st1.hasAction(inputAction)) return false
        val cmpAction = AutomatonAction.tau()
        // st1 -> outputs with st2 -> inputs
        for (outState in state.st1.getDstStates(outputAction)) {
            for (inState in state.st2.getDstStates(inputAction)) {
                // matched -> add new state
                addNewStep(state, cmpAction, CompositeState(outState, inState), queueProvider)
            }
        }

        // st2 -> outputs with st1 -> inputs
        for (outState in state.st2.getDstStates(outputAction)) {
            for (inState in state.st1.getDstStates(inputAction)) {
                // matched -> add new state
                addNewStep(state, cmpAction, CompositeState(inState, outState), queueProvider)
            }
        }
        return true
    }

    private fun addNewStep(state: CompositeState, cmpAction: AutomatonAction, dstState: CompositeState, queueProvider: Consumer<CompositeState>) {
        val src = state.cmpMemState
        val dst = dstState.cmpMemState
        Ulogger.debug(String.format("add step: %s - %s -> %s", src.name, cmpAction, dst.name))
        builder.addStateIfNeeded(dst)
        builder.addAction(cmpAction)
        builder.addStep(src, cmpAction, dst)
        queueProvider.accept(dstState)
    }

    /**
     * Check if 2 automata are composable, Def. 10
     * @return true if composable, otherwise false
     */
    val isParallelComposable: Boolean
        get() = isComposable(
                ia1.inputActions,
                ia1.outputActions,
                ia1.internalActions,
                ia2.inputActions,
                ia2.outputActions,
                ia2.internalActions)
}