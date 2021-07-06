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

package ialib.iam.refinement

import ialib.bes.DocBesFormula
import ialib.core.AbstractAutomaton
import ialib.iam.MemAutomaton
import ialib.iam.MemState
import ialib.iam.expr.solver.DefaultSmtSolver
import ialib.iam.nfasim.DefaultMemSimProvider
import ialib.iam.simulation.SimGraph
import ialib.simpleia.refinement.bes.AbstractBesRefinementOperation
import ialib.solvers.PbesSolver

class MemBesRefinementOperation(private val solver: DefaultSmtSolver, pbesSolver: PbesSolver) : AbstractBesRefinementOperation<MemState>(pbesSolver) {

    var graph: SimGraph? = null
        private set

    override fun buildBesFormula(specificIa: AbstractAutomaton<MemState>, abstractIa: AbstractAutomaton<MemState>): DocBesFormula? {
        return if (abstractIa is MemAutomaton && specificIa is MemAutomaton) {
            val provider = DefaultMemSimProvider(solver, specificIa, abstractIa)
            val gr = provider.start()
            this.graph = gr
            MemBesFormulaBuilder(gr).build()
        } else null
    }
}