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

package swtia.transformation.sir

import com.google.inject.Inject
import swtia.ia.GModel
import swtia.ia.GProc
import swtia.transformation.Transformer
import swtia.util.ResourceUtil.getModel
import swtia.util.Ulogger
import swtia.util.XtextModelHelper

/**
 * Remove control-flow from language
 * Support case and label, jump only
 */
class SimpleIrTransformer: Transformer<GModel, GModel> {

    @Inject
    private lateinit var controlFlowNormalizer: ControlFlowNormalizer

    @Inject
    private lateinit var modelInitNormalizer: ModelInitNormalizer

    @Inject
    private lateinit var flatCmpNormalizer: FlatCmpNormalizer

    @Inject
    private lateinit var modelHelper: XtextModelHelper

    override fun transform(model: GModel): GModel {

        // normalize processes
        transformProc(controlFlowNormalizer, model)
        transformProc(flatCmpNormalizer, model)

        // normalize the init
        modelInitNormalizer.normalize(model.init)
        modelHelper.dumpModel(model, "Final_" + modelInitNormalizer.javaClass.simpleName)

        return model
    }

    private fun transformProc(normalizer: ProcIrNormalizer, model: GModel) {
        Ulogger.debug { "normalizer: ${normalizer.javaClass.simpleName}" }

        // go through each step
        for (proc in model.procs) {
            normalizer.normalize(proc)
        }

        modelHelper.dumpModel(model, normalizer.javaClass.simpleName)
    }
}