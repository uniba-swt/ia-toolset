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

package swtia.transformation.mia

import com.google.inject.Inject
import swtia.ia.GModel
import swtia.sys.mia.MiaSysIa
import swtia.transformation.TransformException
import swtia.transformation.mia.cfg.MiaCfg
import swtia.transformation.mia.cfg.MiaCfgTransformer
import swtia.transformation.mia.cfg.MiaCfgValidator
import swtia.transformation.mir.IrProc
import swtia.transformation.mir.XtextProcToIrProc.Companion.toIrProcWithDefaults
import swtia.transformation.sir.SimpleIrTransformer
import swtia.util.XtextModelHelper

/**
 * full transform controller for IAM
 *
 * IA -> Simple IA (IR) -> CFG -> MIA (via MIA graph)
 */
class MiaTransformationController {

    @Inject
    private lateinit var irTransformer: SimpleIrTransformer

    @Inject
    private lateinit var cfgValidator: MiaCfgValidator

    @Inject
    private lateinit var cfgToMiaTransformer: CfgToMiaTransformer

    @Inject
    private lateinit var miaValidator: MiaValidator

    @Inject
    private lateinit var modelHelper: XtextModelHelper

    @Throws(TransformException::class)
    fun transform(model: GModel): List<MiaSysIa> {
        // 1st: IA -> Simple IA (IR)
        val ir = irTransformer.transform(model)

        // 2nd: Simple IA -> CFG + validate
        val graphs = ir.procs.map {  p ->
            val irProc = p.toIrProcWithDefaults()
            transformIrProcToCfgAndValidate(irProc, false)
        }

        // 3rd: CFG -> IAM for validation
        return graphs.map { gr -> transformToMiaAndValidate(gr, false) }
    }

    /**
     * Create CFG from process
     */
    @Throws(TransformException::class)
    fun transformIrProcToCfgAndValidate(proc: IrProc, atRuntime: Boolean): MiaCfg {
        return MiaCfgTransformer(proc).transform().also { graph ->
            modelHelper.dumpCfg(graph, getPrefix(atRuntime))
            cfgValidator.validate(graph)
        }
    }

    /**
     * transform cfg to ia and validate
     */
    @Throws(TransformException::class)
    fun transformToMiaAndValidate(cfgGraph: MiaCfg, atRuntime: Boolean): MiaSysIa {
        return cfgToMiaTransformer.transform(cfgGraph).also { ia ->
            modelHelper.dumpMia(ia, getPrefix(atRuntime))
            miaValidator.validate(ia)
        }
    }

    private fun getPrefix(atRuntime: Boolean): String {
        return if (atRuntime) { "runtime" } else { "debug" }
    }
}