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

package swtia.sys.mia

import com.google.inject.Inject
import ialib.core.refinement.SimGraphInterface
import ialib.core.services.TextFileWriter
import ialib.mia.ModalCloner
import ialib.mia.prune.ModalPruner
import ialib.mia.scope.ModalScopeOperation
import ialib.mia.product.ModalProductOperation
import ialib.mia.refinement.ModalBesRefinement
import ialib.mia.simulation.DotModalSimController
import swtia.ia.GAction
import swtia.startup.OperatorOptions
import swtia.sys.RuntimeProviderInterface
import swtia.sys.SolverRuntimeUtil
import swtia.transformation.TransformException
import swtia.transformation.mia.MiaTransformationController
import swtia.transformation.mir.IrProc
import swtia.util.InternalIaException
import swtia.util.Ulogger
import java.nio.file.Paths

class MiaRuntimeProvider: RuntimeProviderInterface<MiaSysIa> {

    @Inject
    private lateinit var miaTransformController: MiaTransformationController

    @Throws(TransformException::class)
    override fun createSysFromProc(name: String, proc: IrProc): MiaSysIa {
        val cfg = miaTransformController.transformIrProcToCfgAndValidate(proc, true)
        cfg.name = name
        return miaTransformController.transformToMiaAndValidate(cfg, true)
    }

    override fun prune(name: String, sys: MiaSysIa): MiaSysIa {
        val ia = ModalPruner(sys.automaton, name).prune()
        return MiaSysIa.of(name, ia)
    }

    override fun restrict(name: String, sys: MiaSysIa, args: List<GAction>): MiaSysIa {
        throw InternalIaException("scope is not supported in IAM")
    }

    override fun product(name: String, sys1: MiaSysIa, sys2: MiaSysIa): MiaSysIa {
        val handler = ModalProductOperation()
        val ia = handler.build(sys1.automaton, sys2.automaton)
        return MiaSysIa.of(name, ia)
    }

    override fun refinement(specificSys: MiaSysIa, abstractSys: MiaSysIa): Pair<Boolean, SimGraphInterface?> {
        val textFileWriter = TextFileWriter.default(Paths.get(OperatorOptions.outputPath, "runtime").toString())
        val handler = ModalBesRefinement(SolverRuntimeUtil.loadPbesSolver())
        val result = handler.verify(specificSys.automaton, abstractSys.automaton)
        val graph = handler.graph

        // gen dot if needed
        if (Ulogger.isVerboseOrDebug && graph != null) {
            DotModalSimController(textFileWriter, graph).exec().also { Ulogger.info("generated dot at: $it") }
        }

        return Pair(result, graph)
    }

    override fun copy(name: String, sys: MiaSysIa): MiaSysIa {
        return MiaSysIa(ModalCloner().clone(name, sys.automaton))
    }

    override fun scope(name: String, sys: MiaSysIa, args: List<GAction>): MiaSysIa {
        val ia = ModalScopeOperation().scope(sys.automaton, args.map { a -> a.name }.toSet())
        return MiaSysIa.of(name, ia)
    }
}