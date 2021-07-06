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

package ialib.simpleia.refinement.bes

import ialib.bes.*
import org.apache.log4j.Logger
import ialib.simpleia.Automaton
import ialib.simpleia.simulation.AbstractIASimTraversor
import ialib.simpleia.simulation.SimState
import ialib.bes.AtomBesFormula.Companion.createFalse
import ialib.bes.AtomBesFormula.Companion.createVar
import ialib.core.simulation.SimActionType
import ialib.core.simulation.RefinementType
import java.util.*
import java.util.function.Consumer

class BesFormulaBuilder(specIa: Automaton, implIa: Automaton) : AbstractIASimTraversor(specIa, implIa),
    BesFormulaBuilderInterface {
    private val nuItems: MutableSet<NuFormulaItem>
    private var init: NuFormulaItem? = null

    override fun build(): DocBesFormula? {
        if (start()) {
            return init?.let { DocBesFormula(it, nuItems) }
        }

        return null
    }

    override fun processSimState(state: SimState, queueProvider: Consumer<SimState>) {
        super.processSimState(state, queueProvider)
        // build formula for each state
        val item = buildNuFormulaForState(state, queueProvider)
        if (state.isInit) init = item
        nuItems.add(item)
    }

    private fun buildNuFormulaForState(st: SimState, queueProvider: Consumer<SimState>): NuFormulaItem {
        logger.debug("build NU formula for state: " + st.id)
        // incomplete
        if (st.isIncomplete) {
            return NuFormulaItem(st.id, createFalse())
        }

        // explore the actions (conjunction)
        val forms: MutableList<BesFormulaBase> = ArrayList()
        for (action in st.actions) {

            // build sub-forms from dst
            val subForms: MutableList<BesFormulaBase> = ArrayList()
            for (dst in st.getDstStates(action)) {
                queueProvider.accept(dst)
                subForms.add(createVar(dst.id))
            }

            // input
            if (action.edgeType === RefinementType.Spec) {
                if (action.actionSpec.actionType === SimActionType.Input) {
                    forms.add(BesFormulaFactory.createAnd(subForms))
                }
                continue
            }

            // outputs or internals: create disjunction
            if (action.actionImpl.actionType === SimActionType.Output
                    || action.actionImpl.actionType === SimActionType.Internal) {
                forms.add(BesFormulaFactory.createOr(subForms))
            }
        }

        // create
        return NuFormulaItem(st.id, BesFormulaFactory.createAnd(forms))
    }

    companion object {
        private val logger = Logger.getLogger(BesFormulaBuilder::class.java)
    }

    init {
        nuItems = TreeSet()
    }
}