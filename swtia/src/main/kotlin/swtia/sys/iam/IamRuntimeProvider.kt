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

package swtia.sys.iam

import com.google.inject.Inject
import ialib.core.refinement.SimGraphInterface
import ialib.core.services.TextFileWriter
import ialib.iam.MemAutomaton
import ialib.iam.MemCloner
import ialib.iam.composition.DebugIamProductOperation
import ialib.iam.composition.IamPruner
import ialib.iam.composition.IamRestrictOperation
import ialib.iam.nfasim.DotMemSimController
import ialib.iam.refinement.MemBesRefinementOperation
import swtia.ia.GAction
import swtia.startup.OperatorOptions
import swtia.sys.RuntimeProviderInterface
import swtia.sys.SolverRuntimeUtil
import swtia.transformation.TransformException
import swtia.transformation.iam.IamTransformController
import swtia.transformation.mir.IrProc
import swtia.util.Ulogger
import java.nio.file.Paths

class IamRuntimeProvider: RuntimeProviderInterface<SysIa> {

    @Inject
    private lateinit var iamTransformController: IamTransformController

    @Throws(TransformException::class)
    override fun createSysFromProc(name: String, proc: IrProc): SysIa {
        // convert to CFG
        val cfg = iamTransformController.transformIrProcToCfgAndValidate(proc, true)
        cfg.name = name
        return iamTransformController.transformToIamAndValidate(cfg, true)
    }

    override fun prune(name: String, sys: SysIa): SysIa {
        val pruner = IamPruner(sys.automaton, name)
        return SysIa.of(name, pruner.prune())
    }

    override fun restrict(name: String, sys: SysIa, args: List<GAction>): SysIa {
        val op = IamRestrictOperation()
        val ia: MemAutomaton = op.restrict(sys.automaton, args.map { a -> a.name }.toSet())
        return SysIa.of(name, ia)
    }

    override fun product(name: String, sys1: SysIa, sys2: SysIa): SysIa {
        val solver = SolverRuntimeUtil.loadSmtSolver()
        val handler = DebugIamProductOperation(solver, sys1.automaton, sys2.automaton, name)
        val ia = handler.build()
        return SysIa.of(name, ia)
    }

    override fun refinement(specificSys: SysIa, abstractSys: SysIa): Pair<Boolean, SimGraphInterface?> {
        val textFileWriter = TextFileWriter.default(Paths.get(OperatorOptions.outputPath, "runtime").toString())
        val handler = MemBesRefinementOperation(SolverRuntimeUtil.loadSmtSolver(), SolverRuntimeUtil.loadPbesSolver())

        val specific = specificSys.automaton
        val abstract = abstractSys.automaton
        val result = handler.verify(specific, abstract)
        val graph = handler.graph

        // gen dot if needed
        if (Ulogger.isVerboseOrDebug && graph != null) {
            DotMemSimController(textFileWriter, graph).exec().also { Ulogger.info("generated dot at: $it") }
        }
        return Pair(result, graph)
    }

    override fun copy(name: String, sys: SysIa): SysIa {
        return SysIa(MemCloner().clone(name, sys.automaton))
    }
}