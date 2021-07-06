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

import ialib.Ulogger
import ialib.core.refinement.RefinementUtil.isInputOutputValid
import ialib.core.simulation.AbstractSimTraversor
import ialib.mia.ModalAutomaton
import ialib.mia.refinement.ModalSimGraph
import java.util.function.Consumer

abstract class AbstractModalSimTraversor(private val specIa: ModalAutomaton, private val implIa: ModalAutomaton)
    : AbstractSimTraversor<ModalSimState>() {

    private val simulator = ModalNfaStepSimulator()

    protected lateinit var initState: ModalSimState

    private val lstStates = mutableListOf<ModalSimState>()

    fun start(): ModalSimGraph {
        if (shouldCheckIO()) {
            if (!isInputOutputValid(specIa, implIa)) {
                Ulogger.error("Input and output set are not valid")
                error("Input and output set are not valid")
            }
        }

        initState = ModalSimState(specIa.initState, implIa.initState)
        start(initState)
        val name = "${implIa.name}_${specIa.name}"
        return ModalSimGraph(name, lstStates, initState.id, implIa, specIa)
    }

    protected open fun shouldCheckIO() = true

    override fun processSimState(state: ModalSimState, queueProvider: Consumer<ModalSimState>) {
        simulator.updateStepsToState(state)
        lstStates.add(state)
        postProcessSimState(state) { s -> queueProvider.accept(s)}
    }

    abstract fun postProcessSimState(state: ModalSimState, queueProvider: (ModalSimState) -> Unit)
}