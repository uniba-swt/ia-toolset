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

package ialib.mia.refinement

import ialib.bes.*
import ialib.mia.simulation.*
import java.util.*

class ModalBesFormulaBuilder(private val graph: ModalSimGraph) : BesFormulaBuilderInterface {

    private val nuItems = TreeSet<NuFormulaItem>()
    private var init: NuFormulaItem? = null

    override fun build(): DocBesFormula? {
        for (state in graph.states) {
            processItem(state)
        }
        return init?.let { DocBesFormula(it, nuItems) }
    }

    private fun processItem(item: ModalSimState) {
        // build formula for each state
        val frm: NuFormulaItem = buildNuFormulaForState(item)
        if (item.isInit)
            init = frm

        nuItems.add(frm)
    }

    private fun buildNuFormulaForState(state: ModalSimState): NuFormulaItem {
        // incomplete
        if (state.isIncomplete) {
            return NuFormulaItem(state.id, AtomBesFormula.createFalse())
        }

        val items = state.steps.values.map { bes -> buildBes(bes)}
        return NuFormulaItem(state.id, BesFormulaFactory.createAnd(items))
    }

    private fun buildBes(bes: SimBesBase): BesFormulaBase {
        return when (bes) {
            is ModalSimState -> {
                return AtomBesFormula.createVar(bes.id)
            }
            is FalseSimBes -> AtomBesFormula.createFalse()
            is CollectionSimBes -> {
                val items = bes.items.map { i -> buildBes(i) }
                return if (bes is OrSimBes) {
                    BesFormulaFactory.createOr(items)
                } else {
                    BesFormulaFactory.createAnd(items)
                }
            }
            else -> throw Exception("$bes is not supported")
        }
    }
}