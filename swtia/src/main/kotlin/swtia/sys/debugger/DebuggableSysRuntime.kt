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

package swtia.sys.debugger

import ialib.debug.IaScope
import ialib.debug.IaStackFrame
import ialib.util.EventBus
import ialib.util.StmtBusMessage
import ialib.util.StmtBusType
import swtia.ia.GModelInit
import swtia.ia.GSysStmt
import swtia.sys.IaRuntimeException
import swtia.sys.runtime.InternalSysRuntime
import swtia.util.ResourceUtil.getLocation

class DebuggableSysRuntime: InternalSysRuntime() {

    val initScope = IaScope.ofInit()

    private val initFrameId = 1

    val frame = IaStackFrame(initFrameId, "init")

    override fun onExceptionCaught(ex: IaRuntimeException) {
        EventBus.publish(StmtBusMessage(StmtBusType.Exception, ex.message))
    }

    override fun onStmtWillBegin(stmt: GSysStmt) {
        frame.updateLocation(stmt.getLocation())
        EventBus.publish(StmtBusMessage(StmtBusType.StmtWillStart, stmt))
    }

    override fun onStmtEnded(stmt: GSysStmt) {
        EventBus.publish(StmtBusMessage(StmtBusType.StmtEnded, stmt))
    }

    override fun onInitWillBegin(init: GModelInit) {
        EventBus.publish(StmtBusMessage(StmtBusType.InitWillStart, init))
    }

    override fun onInitEnded(init: GModelInit) {
        val loc = init.getLocation()
        frame.updateLocation(loc.toEndOnly())
        EventBus.publish(StmtBusMessage(StmtBusType.InitEnded, init))
    }

    fun getScopes(frameId: Int): List<IaScope> {
        return if (frameId == initFrameId) {
            listOf(initScope)
        } else {
            emptyList()
        }
    }
}

