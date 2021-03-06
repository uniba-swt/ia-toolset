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

import ialib.core.refinement.RefinementUtil
import ialib.core.simulation.AbstractSimTraversor
import ialib.iam.MemAutomaton
import ialib.iam.expr.solver.DefaultSmtSolver
import java.util.function.Consumer

abstract class AbstractMemSimTraversor protected constructor(solver: DefaultSmtSolver, private val specificIa: MemAutomaton, private val abstractIa: MemAutomaton)
    : AbstractSimTraversor<SimMemState>() {

    private val simulator: MemNfaStepSimulator = MemNfaStepSimulator(solver)

    protected lateinit var initState: SimMemState

    private val lstStates = mutableListOf<SimMemState>()

    /**
     * start process and build completed sim state
     */
    fun start(): SimGraph {
        if (!RefinementUtil.isInputOutputEqual(specificIa, abstractIa)) {
            error("Input and output sets are not equal")
        }
        initState = SimMemState(abstractIa.initState, specificIa.initState)
        start(initState)
        val name = "${specificIa.name}_${abstractIa.name}"
        return SimGraph(name, lstStates, initState.id, specificIa, abstractIa)
    }

    override fun processSimState(state: SimMemState, queueProvider: Consumer<SimMemState>) {
        simulator.updateStepsToState(state)
        lstStates.add(state)
        postProcessSimState(state) { s -> queueProvider.accept(s) }
    }

    abstract fun postProcessSimState(state: SimMemState, queueProvider: (SimMemState) -> Unit)
}