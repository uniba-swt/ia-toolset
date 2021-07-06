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

package swtia.ide.lsp

import swtia.ide.json.IamJsonModel
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment
import swtia.ide.json.SimGraphJson
import java.util.concurrent.CompletableFuture

@JsonSegment("ia-request")
interface IaRequestEndpoint {

    @JsonRequest
    fun runInit(msg: Array<RunRequestMsg>): CompletableFuture<RunResponseMsg>

    @JsonRequest
    fun simulateRefinement(msg: Array<SimulateRefineRequestMsg>): CompletableFuture<SimulateRefineResponseMsg>

    @JsonRequest
    fun simulateErrorRefinement(msg: Array<SimulateRefineRequestMsg>): CompletableFuture<SimulateRefineResponseMsg>

    @JsonRequest
    fun exploreProc(msg: Array<ExploreProcRequestMsg>): CompletableFuture<ExploreProcResponseMsg>
}

class RunRequestMsg {
    var uri: String = ""
}

class RunResponseMsg(success: Boolean, errorMsg: String?): ResponseMsgBase(success, errorMsg)

class SimulateRefineRequestMsg {
    val uri: String = ""
}

class SimulateRefineResponseMsg(success: Boolean, errorMsg: String?, val data: SimGraphJson?): ResponseMsgBase(success, errorMsg)

class ExploreProcRequestMsg {
    val uri: String = ""
    val procName: String = ""
}

class ExploreProcResponseMsg(success: Boolean, errorMsg: String?, val data: IamJsonModel?): ResponseMsgBase(success, errorMsg)

open class ResponseMsgBase(val success: Boolean, val errorMsg: String?)