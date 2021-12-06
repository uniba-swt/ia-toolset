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

package ialib.simpleia.refinement.mcrl2

import ialib.Ulogger
import ialib.simpleia.Automaton
import ialib.core.AutomatonAction
import ialib.core.refinement.RefinementUtil
import ialib.core.services.TextFileWriter
import ialib.simpleia.refinement.formula.FormulaBuilder
import ialib.solvers.DefaultCliExecutor
import java.util.*

/**
 * Refinement checking with mCRL2 model and formula
 * - Convert IA Spec to .mcf formula (greatest fix-point operator)
 * - Convert IA Impl to .mcrl2 (and to .lps afterwards)
 * - Build .pbes from .mcf and .lps using external tool
 * - Solve .pbes using external tool
 *
 * - SLOW with large amount of states, actions
 */
class FormulaRefinementOperation(val ia1: Automaton, val ia2: Automaton) {
    fun verify(fsa: TextFileWriter, executor: DefaultCliExecutor): Boolean {
        // check input/output set
        if (!RefinementUtil.isInputOutputEqual(ia1, ia2)) return false

        // Step 1: build formula for IA 1
        val frm = FormulaBuilder(ia1).buildMcfString()
        Ulogger.debug(frm)

        // Step 2: dump mCRL2 HML source code from formula
        val mcfFile = ia1.name + ".mcf"
        Ulogger.info("Write Spec to file $mcfFile")
        fsa.generateFile(mcfFile, frm)

        // Step 3: generate mCRL2 model from IA 2

        // pre-process by adding placeholder for missing outputs
        var iaImpl = ia2
        if (ia2.outputActions.size < ia1.outputActions.size) {
            iaImpl = cloneIaWithAdditionalOutputs(ia2, ia1.outputActions)
        }
        val mcrl2 = MCrl2ModelBuilder(iaImpl).build()
        Ulogger.debug(mcrl2)
        val mcrl2File = iaImpl.name + ".mcrl2"
        val lpsFile = iaImpl.name + ".lps"
        Ulogger.info("Write Impl to file $mcrl2File")
        fsa.generateFile(mcrl2File, mcrl2)

        // Step 4: call command line tool to verify formula
        val pbesFile = String.format("combine_%s_%s.pbes", ia1.name, iaImpl.name)
        return execMcrl2Tools(fsa, executor, mcrl2File, lpsFile, mcfFile, pbesFile)
    }

    companion object {
        private fun execMcrl2Tools(fsa: TextFileWriter, executor: DefaultCliExecutor, mcrl2Name: String, lpsName: String, mcfName: String, pbesName: String): Boolean {
            val cmds = arrayOf(String.format("mcrl22lps -z %s -o %s", mcrl2Name, lpsName), String.format("lps2pbes -u -f %s %s %s", mcfName, lpsName, pbesName))
            val dir = fsa.outputPath
            for (cmd in cmds) {
                val res = executor.exec(cmd, dir)
                if (!res.first) return false
            }

            // last step
            val cmd2 = String.format("pbes2bool %s", pbesName)
            val res2 = executor.exec(cmd2, dir)
            return res2.first && "true".equals(res2.second, ignoreCase = true)
        }

        private fun cloneIaWithAdditionalOutputs(ia: Automaton, outputActions: Set<AutomatonAction>): Automaton {
            val outSet: MutableSet<AutomatonAction> = HashSet()
            outSet.addAll(ia.outputActions)
            outSet.addAll(outputActions)
            return Automaton(ia.name, ia.inputActions, outSet, ia.internalActions, ia.states, ia.initState)
        }
    }
}