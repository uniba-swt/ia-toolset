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

package swtia.debugger.iad

import ialib.iam.product.debug.MemProductTraceRecord
import org.eclipse.lsp4j.debug.StoppedEventArguments
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason
import org.eclipse.lsp4j.debug.TerminatedEventArguments
import swtia.debugger.IaDebugProtocolClient
import swtia.debugger.IaDebugProtocolServer
import swtia.debugger.iad.models.ProductTracesNotification.Companion.toProductTracesNotification
import swtia.sys.debugger.ClientListener

class ClientWrapper(private val client: IaDebugProtocolClient): ClientListener {

    fun initialized() {
        client.initialized()
    }

    override fun stopOnEntry() {
        internalStop(StoppedEventArgumentsReason.ENTRY)
    }

    override fun stopOnStep() {
        internalStop(StoppedEventArgumentsReason.STEP)
    }

    override fun stopOnBreakpoint() {
        internalStop(StoppedEventArgumentsReason.BREAKPOINT)
    }

    override fun stopOnException(text: String) {
        internalStop(StoppedEventArgumentsReason.EXCEPTION, text)
    }

    override fun terminate() {
        client.terminated(TerminatedEventArguments())
    }

    override fun pause(desc: String, text: String) {
        client.stopped(StoppedEventArguments().also { st ->
            st.threadId = IaDebugProtocolServer.threadId
            st.reason = StoppedEventArgumentsReason.EXCEPTION
            st.description = desc
            st.text = text
        })
    }

    override fun productTrace(records: List<MemProductTraceRecord>) {
        client.productTraces(records.toProductTracesNotification())
    }

    override fun productEnd() {
        client.productEnd()
    }

    private fun internalStop(reason: String, text: String = "") {
        client.stopped(StoppedEventArguments().also { st ->
            st.threadId = IaDebugProtocolServer.threadId
            st.reason = reason
            st.text = text
        })
    }
}