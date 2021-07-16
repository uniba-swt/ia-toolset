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

package swtia.ide

import ialib.iam.simulation.SimGraph
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.xtext.ide.server.LanguageServerImpl
import swtia.ide.json.ErrorSimGraphJsonFactory
import swtia.ide.json.IamJsonModel
import swtia.ide.json.SimGraphJsonFactory.toSimGraphJson
import swtia.ide.lsp.*
import swtia.startup.AppFactory
import swtia.startup.StandaloneApp
import java.util.concurrent.CompletableFuture

/**
 * Custom LSP language server with new endpoint
 */
class IaRequestLanguageServer : LanguageServerImpl(), IaRequestEndpoint {

    private fun createStandaloneApp(): StandaloneApp = AppFactory.createStandaloneApp()

    override fun createServerCapabilities(params: InitializeParams?): ServerCapabilities {
        val serv = super.createServerCapabilities(params)
        serv.completionProvider.triggerCharacters = listOf(" ", ";", "=")
        return serv
    }

    override fun runInit(msg: Array<RunRequestMsg>): CompletableFuture<RunResponseMsg> {
        return CompletableFuture.supplyAsync {
            val runtimeResult = createStandaloneApp().execRuntime(msg.first().uri)
            RunResponseMsg(runtimeResult.isSat, runtimeResult.errorMsg)
        }
    }

    override fun simulateRefinement(msg: Array<SimulateRefineRequestMsg>): CompletableFuture<SimulateRefineResponseMsg> {
        val req = msg.first()
        return CompletableFuture.supplyAsync {
            val runtimeResult = createStandaloneApp().execRuntime(req.uri)
            val data = runtimeResult.runtimeData?.graphs?.filterIsInstance<SimGraph>()?.firstOrNull()?.toSimGraphJson()
            SimulateRefineResponseMsg(runtimeResult.isSat, runtimeResult.errorMsg, data)
        }
    }

    override fun simulateErrorRefinement(msg: Array<SimulateRefineRequestMsg>): CompletableFuture<SimulateRefineResponseMsg> {
        val req = msg.first()
        return CompletableFuture.supplyAsync {
            val runtimeResult = createStandaloneApp().execRuntime(req.uri)
            val gr = runtimeResult.runtimeData?.graphs?.filterIsInstance<SimGraph>()?.firstOrNull()
            val data = gr?.let {  ErrorSimGraphJsonFactory.toSimGraphJson(it) }
            SimulateRefineResponseMsg(runtimeResult.isSat, runtimeResult.errorMsg, data)
        }
    }

    override fun exploreProc(msg: Array<ExploreProcRequestMsg>): CompletableFuture<ExploreProcResponseMsg> {
        val req = msg.first()
        return CompletableFuture.supplyAsync {
            handleExploreProc(req)
        }
    }

    private fun handleExploreProc(req: ExploreProcRequestMsg): ExploreProcResponseMsg {
        // parse and validate
        val standaloneApp = AppFactory.createStandaloneApp()
        val (model, err) = standaloneApp.parseAndValidate(req.uri)
        if (model == null || err != null) {
            return ExploreProcResponseMsg(false, err, null)
        }

        val controller = AppFactory.createIamTransformController()
        val ia = controller.transformSingleProc(model, req.procName)
        val json = IamJsonModel.from(ia.name, ia.automaton)
        return ExploreProcResponseMsg(true, null, json)
    }
}