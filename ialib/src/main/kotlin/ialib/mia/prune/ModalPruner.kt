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

package ialib.mia.prune

import ialib.core.pruner.PrunerBase
import ialib.mia.ModalAutomaton
import ialib.mia.ModalAutomatonBuilder
import ialib.mia.ModalState

class ModalPruner(ia: ModalAutomaton, private val name: String): PrunerBase<ModalAutomaton, ModalState>(ia) {

    override fun getAutonomousDstStates(src: ModalState): Sequence<ModalState> {
        return sequence {
            for (action in src.actionsSequence) {
                if (action.isOutputOrInternal()) {
                    for (dst in src.getStepsSequence(action)) {
                        for (st in dst.states) {
                            yield(st)
                        }
                    }
                }
            }
        }
    }

    override fun getInitState(): ModalState {
        return ia.initState
    }

    override fun rebuildIa(errorStates: Set<String>): ModalAutomaton {
        val builder = ModalAutomatonBuilder(name, ia.initState.name)
        for (st in ia.getIterator()) {
            if (errorStates.contains(st.name))
                continue

            // build child
            for (action in st.actionsSequence) {

                // must
                for (step in st.getMustSteps(action)) {
                    val newDstStates = step.states.filter { s ->
                        !(action.isOutputOrInternal() && errorStates.contains(s.name))
                    }.map { s -> s.name }
                    if (newDstStates.isNotEmpty()) {
                        builder.addMustTransition(st.name, step.action, newDstStates)
                    }
                }

                // may
                for (step in st.getMaySteps(action)) {
                    if (!(action.isOutputOrInternal() && errorStates.contains(step.single.name))) {
                        builder.addMayTransition(st.name, step.action, step.single.name)
                    }
                }
            }
        }
        return builder.build()
    }
}