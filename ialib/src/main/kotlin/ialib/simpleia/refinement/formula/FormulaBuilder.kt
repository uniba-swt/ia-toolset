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

package ialib.simpleia.refinement.formula

import org.apache.log4j.Logger
import ialib.simpleia.Automaton
import ialib.core.AutomatonAction
import ialib.simpleia.AutomatonState
import ialib.simpleia.simulation.ClosureSetProvider
import ialib.simpleia.refinement.formula.AtomForm.Companion.createIoActionForm
import ialib.simpleia.refinement.formula.AtomForm.Companion.createTauForm
import java.util.*
import java.util.stream.Collectors

/**
 * Construct formula for a Interface Automaton by iterating over the set of initial states
 */
class FormulaBuilder(private val automaton: Automaton) {
    private val closureSetProvider: ClosureSetProvider = ClosureSetProvider()
    private val queueForms: Queue<Triple<StateForm, AutomatonState, StateForm?>> = ArrayDeque()

    /**
     * Build formula
     * @return formula
     */
    fun build(): StateForm {

        // add first to stack
        val frm = StateForm(automaton.initState.name)
        addToQueue(frm, automaton.initState, null)
        update()

        // build state now
        return frm
    }

    fun buildMcfString(): String {
        return FormulaStringBuilder().toMcfString(build())
    }

    private fun addToQueue(frm: StateForm, state: AutomatonState, parent: StateForm?) {
        queueForms.offer(Triple(frm, state, parent))
    }

    private fun update() {
        while (!queueForms.isEmpty()) {
            val tup = queueForms.remove()
            updateStateForm(tup.first, tup.second, tup.third)
        }
    }

    /**
     * Construct formula for a specific state
     * Conjunction of
     * + Required: Set of input actions (diamond)
     * + Optional: Set of output actions from e-closure set (box)
     * + Optional: Internal actions to e-closure set (box)
     *
     * @param state state
     */
    private fun updateStateForm(current: StateForm, state: AutomatonState, parent: StateForm?) {
        logger.debug("start updateStateForm: " + current.name)

        // update list
        current.updateBoundedStateVariables(parent)

        // input actions
        val conjunctiveInputSet = buildInputConjunctiveSet(state, current)

        // output actions
        val closureStateSet = closureSetProvider.getOrComputeEClosureSet(state)
        val conjunctiveOutputSet = buildOutputConjunctiveSet(current, closureStateSet)

        // tau actions to e-closure
        val tauForm = buildTauFormForClosure(current, closureStateSet)

        // construct state form
        val resultSet: MutableList<BaseForm> = ArrayList(conjunctiveInputSet)
        resultSet.addAll(conjunctiveOutputSet)
        resultSet.add(tauForm)
        if (resultSet.size > 0) {
            current.setBody(CollectionForm(resultSet, CollectionForm.OpType.And))
        } else {
            current.setBody(BooleanForm(BooleanForm.BoolConstant.True))
        }
        logger.debug("finish buildNewState: " + state.name)
    }

    private fun buildInputConjunctiveSet(state: AutomatonState, current: StateForm): List<DiamondForm> {
        val result: MutableList<DiamondForm> = ArrayList()
        for (a in state.inputActions) {
            val dstState = state.getInputDstState(a)
            if (dstState != null) {
                val frm = DiamondForm(createIoActionForm(a.name), buildState(dstState, current))
                result.add(frm)
            }
        }
        return result
    }

    private fun buildTauFormForClosure(current: StateForm, closureStateSet: Set<AutomatonState>): BoxForm {
        val disjunctiveTauSet = closureStateSet.stream().map { s: AutomatonState -> buildState(s, current) }.collect(Collectors.toList())
        return BoxForm(createTauForm(), CollectionForm(disjunctiveTauSet, CollectionForm.OpType.Or))
    }

    private fun buildOutputConjunctiveSet(current: StateForm, closureStateSet: Set<AutomatonState>): List<BaseForm> {
        // e-closure
        val set: MutableList<BaseForm> = ArrayList()
        val outputActionDstMap = getOutputAndDstStates(closureStateSet)
        for ((key, value) in outputActionDstMap) {
            val disConjunctiveDstSet = value.stream().map { s: AutomatonState -> buildState(s, current) }.collect(Collectors.toList())
            val dstOrForm = CollectionForm(disConjunctiveDstSet, CollectionForm.OpType.Or)
            val f = BoxForm(createIoActionForm(key.name), dstOrForm)
            set.add(f)
        }

        // non-allowed output actions
        val setOutput = outputActionDstMap.keys
        for (ioAction in automaton.outputActions) {
            if (!setOutput.contains(ioAction)) {
                val frm = BoxForm(createIoActionForm(ioAction.name), BooleanForm(BooleanForm.BoolConstant.False))
                set.add(frm)
            }
        }
        return set
    }

    private fun getOutputAndDstStates(closureStateSet: Set<AutomatonState>): Map<AutomatonAction, MutableSet<AutomatonState>> {
        val map: MutableMap<AutomatonAction, MutableSet<AutomatonState>> = mutableMapOf()

        // go through closure set
        for (state in closureStateSet) {
            // go through all output actions
            for (action in state.outputActions) {
                for (successor in state.getDstStates(action)) {
                    // init subset
                    if (!map.containsKey(action)) {
                        map[action] = HashSet()
                    }

                    // build subset
                    val set = map[action]!!
                    set.add(successor)
                }
            }
        }
        return map
    }

    /**
     * Build formula for state by first checking bounded variables and construct new if missing
     * @param state state
     * @param parent parent formula
     * @return formula
     */
    private fun buildState(state: AutomatonState, parent: StateForm?): BaseForm {
        if (parent != null && parent.isBoundedState(state.name)) {
            return BoundedVarForm(state.name)
        }
        val frm = StateForm(state.name)
        addToQueue(frm, state, parent)
        return frm
    }

    private class FormulaStringBuilder {
        private val stack: Stack<McfTemplate> = Stack()
        fun toMcfString(frm: StateForm): String {
            updateTemplate(frm, null)
            val strBuilder = visitQueue()
            return strBuilder.toString()
        }

        private fun updateTemplate(frm: BaseForm, parent: BaseForm?) {
            val lst = frm.getTemplates(parent)

            // add to stack in reverse order
            for (i in lst.indices.reversed()) {
                stack.push(lst[i])
            }
        }

        private fun visitQueue(): StringBuilder {
            val strBuilder = StringBuilder()
            while (!stack.isEmpty()) {
                val item = stack.pop()
                if (item.form == null) {
                    strBuilder.append(item.rawStr)
                } else {
                    updateTemplate(item.form, item.parent)
                }
            }
            return strBuilder
        }

    }

    companion object {
        private val logger = Logger.getLogger(FormulaBuilder::class.java)
    }
}