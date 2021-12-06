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

import ialib.bes.DocBesFormula
import ialib.core.AbstractAutomaton
import ialib.core.AbstractState
import ialib.core.CoreException
import ialib.core.refinement.RefinementUtil
import ialib.solvers.PbesSolver
import org.apache.log4j.Logger

abstract class AbstractBesRefinementOperation<T : AbstractState> protected constructor(private val pbesSolver: PbesSolver) {

    protected val logger: Logger = Logger.getLogger(javaClass)

    fun verify(specificIa: AbstractAutomaton<T>, abstractIa: AbstractAutomaton<T>): Boolean {
        logger.info("refinement checking with BES")

        // check input/output set
        if (shouldCheckIO()) {
            if (!RefinementUtil.isInputOutputEqual(specificIa, abstractIa)) {
                val s1 = (specificIa.inputActions + specificIa.outputActions).joinToString { act -> act.formatted() }
                val s2 = (abstractIa.inputActions + abstractIa.outputActions).joinToString { act -> act.formatted() }
                throw CoreException("Input and output sets are not equal ($s1 -- $s2)")
            }
        }

        // Step1. generate BES
        logger.info("start building BES formula")
        val form = buildBesFormula(specificIa, abstractIa) ?: return false

        // Step 2: Call Pbes parser
        val result = pbesSolver.solve(form.toBesText())
        logger.info("PBES solver result: $result")
        return result
    }

    protected open fun shouldCheckIO(): Boolean {
        return true
    }

    protected abstract fun buildBesFormula(specificIa: AbstractAutomaton<T>, abstractIa: AbstractAutomaton<T>): DocBesFormula?

}