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

package swtia.startup

import com.google.inject.Inject
import com.google.inject.Provider
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.xtext.generator.GeneratorDelegate
import org.eclipse.xtext.generator.JavaIoFileSystemAccess
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.validation.CheckMode
import org.eclipse.xtext.validation.IResourceValidator
import swtia.ia.GModel
import swtia.sys.IasSysRuntime
import swtia.sys.runtime.InternalSysRuntime
import swtia.util.ResourceUtil.getRootModel
import swtia.util.Ulogger
import swtia.util.Ulogger.debug
import swtia.util.XtextModelHelper
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.exists

class StandaloneApp {

    @Inject
    private lateinit var validator: IResourceValidator

    @Inject
    private lateinit var resourceSetProvider: Provider<ResourceSet>

    @Inject
    private lateinit var genFsa: JavaIoFileSystemAccess

    @Inject
    private lateinit var runtime: IasSysRuntime

    @Inject
    private lateinit var formattingProvider: FormattingProvider

    @Inject
    private lateinit var modelHelper: XtextModelHelper

    init {
        Ulogger.setup(OperatorOptions.isDebug, OperatorOptions.isVerbose)
    }

    fun execRuntime(file: String, rst: InternalSysRuntime? = null): StandaloneAppResult {
        if (!File(file).exists()) {
            return StandaloneAppResult(false, null, "File does not exist: $file")
        }

        // format -> stop
        if (OperatorOptions.isFormat) {
            debug("format: $file")
            return format(file)
        } else {
            // Load the resource and validate
            debug("run standalone app: $file")
            val (model, err) = parseAndValidate(file)
            if (model == null || err != null) {
                return StandaloneAppResult.unsat(err)
            }
            return execModel(model, rst)
        }
    }

    fun execModel(model: GModel, rst: InternalSysRuntime? = null): StandaloneAppResult {
        // generate
        genFsa.setOutputPath(OperatorOptions.outputPath)

        // process init
        var runtimeResult = false
        var error: String? = null
        val rt = rst ?: runtime
        try {
            rt.execute(model.init)
            runtimeResult = true
        } catch (ex: Exception) {
            error = ex.message ?: ""
            Ulogger.printError("runtime error", error)
        }

        return StandaloneAppResult(runtimeResult, rt.data, error)
    }

    fun parseAndValidate(file: String): Pair<GModel?, String?> {
        val res = parseResource(file)
        val list = validator.validate(res, CheckMode.ALL, CancelIndicator.NullImpl)
        if (list.isNotEmpty()) {
            for (issue in list) {
                System.err.println(issue)
            }
            return Pair(res.getRootModel(), list.joinToString(System.lineSeparator()))
        }

        return Pair(res.getRootModel(), null)
    }

    private fun format(file: String): StandaloneAppResult {
        Ulogger.info("start formatting")
        val res = parseResource(file)
        val model = res.getRootModel() ?: return StandaloneAppResult.unsat()
        formattingProvider.format(file, model)
        Ulogger.info("finished formatting the input files, stop.")
        return StandaloneAppResult.sat()
    }

    private fun parseResource(file: String): Resource {
        return resourceSetProvider.get().getResource(URI.createFileURI(file), true)
    }
}

