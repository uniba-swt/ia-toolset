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

package swtia.debugger

import org.eclipse.lsp4j.debug.*
import swtia.sys.debugger.IaDebugger
import ialib.iam.debug.IaBreakpoint
import ialib.iam.debug.IaScope
import ialib.iam.debug.IaStackFrame
import kotlin.io.path.Path

object Converter {
    fun SetBreakpointsArguments.toIaBreakpoints(): List<IaBreakpoint> {
        return this.breakpoints.map { br ->
            IaBreakpoint(br.line)
        }
    }

    fun Collection<IaBreakpoint>.toBreakpointsResponse() : SetBreakpointsResponse {
        val items = this.map { br -> Breakpoint().apply {
            line = br.line
            isVerified = true
        } }.toTypedArray()
        return SetBreakpointsResponse().apply {
            breakpoints = items
        }
    }

    fun IaDebugger.getSource(): Source {
        return Source().also { src ->
            src.path = this.path
            src.name = Path(this.path).fileName?.toString()
        }
    }

    fun List<IaStackFrame>.toStackTraceResponse(src: Source): StackTraceResponse {
        val items = this.map { frame ->
            StackFrame().apply {
                id = frame.id
                name = frame.name
                line = frame.loc.lineBegin
                endLine = frame.loc.lineEnd
                column = frame.loc.colBegin
                endColumn = frame.loc.colEnd
                source= src
            }
        }.toTypedArray()

        return StackTraceResponse().apply {
            stackFrames = items
        }
    }

    fun List<IaScope>.toScopeResponse(): ScopesResponse {
        val items = this.map { sc ->
            Scope().apply {
                name = sc.name
                variablesReference = sc.variablesReference
                isExpensive = false
            }
        }.toTypedArray()
        return ScopesResponse().apply {
            scopes = items
        }
    }
}