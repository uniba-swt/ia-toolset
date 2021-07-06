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

import ialib.simpleia.Automaton
import ialib.core.refinement.RefinementUtil.isInputOutputValid
import ialib.core.simulation.AbstractSimTraversor
import java.util.function.Consumer

abstract class AbstractIASimTraversor protected constructor(private val specIa: Automaton, private val implIa: Automaton) : AbstractSimTraversor<SimState>() {
    private val simulator = NfaStepSimulator()
    protected fun start(): Boolean {
        if (!isInputOutputValid(specIa, implIa)) {
            return false
        }
        start(SimState(specIa.initState, implIa.initState))
        return true
    }

    override fun processSimState(state: SimState, queueProvider: Consumer<SimState>) {
        simulator.updateStepsToState(state)
    }
}