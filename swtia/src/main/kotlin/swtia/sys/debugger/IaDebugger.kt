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

import com.google.inject.Inject
import ialib.iam.composition.debug.MemProductStackFrame
import ialib.iam.debug.DebugSession
import ialib.iam.debug.IaBreakpoint
import ialib.iam.debug.IaScope
import ialib.iam.debug.IaStackFrame
import ialib.util.*
import kotlinx.coroutines.*
import org.apache.log4j.Logger
import swtia.ia.*
import swtia.startup.AppFactory
import swtia.startup.StandaloneApp
import swtia.sys.models.SysIaBase
import java.util.*

class IaDebugger {

    private val logger = Logger.getLogger(IaDebugger::class.java)

    @Inject
    private lateinit var standaloneApp: StandaloneApp

    @Inject
    private lateinit var runtime: DebuggableSysRuntime

    private var stopOnEntry: Boolean = false

    private var breakpoints = mutableMapOf<Int, IaBreakpoint>()

    private var isStepOver: Boolean = false

    private var isInitEnded: Boolean = false

    private var client: ClientListener? = null

    private var _lastResumeLine = 0

    private var job: Job? = null

    val stackFrames = Stack<IaStackFrame>()

    var path: String = ""
        private set

    fun start(path: String, stopOnEntry: Boolean, listener: ClientListener?) {

        // update value
        this.path = path
        this.stopOnEntry = stopOnEntry
        this.client = listener
        EventBus.subscribe(this::handleMessage)

        // stackFrames
        stackFrames.push(runtime.frame)

        // mark new session
        DebugSession.newSession()

        // start
        job = CoroutineScope(Dispatchers.Default).launch {
            standaloneApp.execRuntime(path, runtime)
            logger.debug("finished debugging")
        }
    }

    private fun handleMessage(msg: EventBusMessage) {
        when (msg) {
            is ProductBusMessage -> {
                when (msg.type) {
                    ProductBusType.WillStart -> {
                        stackFrames.push(msg.sender.frame)
                    }
                    ProductBusType.Ended -> {
                        stackFrames.pop()
                        client?.productEnd()
                    }
                    ProductBusType.WillProcessState -> stopOnBreakpointOrStepOrElse {}
                    ProductBusType.EndedProcessState -> {}
                    ProductBusType.ErrorState -> {
                        client?.pause("PRODUCT ERROR", msg.msg ?: "Error state found in product")
                        onStopped()
                        DebugSession.current.pause()
                    }
                }
            }
            is StmtBusMessage -> {
                when (msg.type) {
                    StmtBusType.InitWillStart -> {
                        if (stopOnEntry) {
                            pauseRuntime()
                            client?.stopOnEntry()
                            onStopped()
                        }
                    }
                    StmtBusType.InitEnded -> {
                        stopOnBreakpointOrStepOrElse { terminate() }
                        isInitEnded = true
                    }
                    StmtBusType.StmtWillStart -> stopOnBreakpointOrStepOrElse {}
                    StmtBusType.StmtEnded -> {}
                    StmtBusType.Exception -> {
                        val text = msg.arg?.let { if (it is String) { it } else {""} } ?: ""
                        client?.stopOnException(text)
                        onStopped()
                    }
                    StmtBusType.Shutdown -> terminate()
                }
            }
        }
    }

    private fun terminate() {
        client?.terminate()
        gracefulShutdown()
    }

    fun disconnect() {
        gracefulShutdown()
    }

    private fun gracefulShutdown() {
        DebugSession.current.shutdown()
    }

    private fun stopOnBreakpointOrStepOrElse(elseProd: () -> Unit) {
        when {
            checkBreakpoint() -> {
                pauseRuntime()
                client?.stopOnBreakpoint()
                onStopped()
            }
            isStepOver -> {
                this.pauseRuntime()
                client?.stopOnStep()
                onStopped()
            }
            else -> elseProd()
        }
    }

    private fun onStopped() {
        val frame = stackFrames.peek()
        if (frame is MemProductStackFrame) {
            client?.productTrace(frame.traces.asReversed())
        }
    }

    fun runContinue() {
        isStepOver = false
        this.resumeRuntime()
    }

    fun stepOver() {
        isStepOver = true
        this.resumeRuntime()
    }

    fun getScopes(frameId: Int): List<IaScope> {
        return runtime.getScopes(frameId)
    }

    fun getDeclaredAutomata(scopeRef: Int): List<SysIaBase>? {
        return if (runtime.initScope.variablesReference == scopeRef) {
            return runtime.data.map.values.filter { ia -> !ia.name.startsWith("_") }
        } else null
    }

    fun stepIn() {
        // do not support
        runContinue()
    }

    fun stepOut() {
        // do not support
        runContinue()
    }

    private fun pauseRuntime() {
        DebugSession.current.pause()
    }

    private fun resumeRuntime() {
        _lastResumeLine = currentFrame().loc.lineBegin
        DebugSession.current.resume()
        if (isInitEnded) {
            client?.terminate()
        }
    }

    fun updateBreakpoints(list: List<IaBreakpoint>): Collection<IaBreakpoint> {
        breakpoints.clear()
        list.forEach { item ->
            breakpoints[item.line] = item
        }
        return breakpoints.values
    }

    private fun currentFrame(): IaStackFrame {
        return stackFrames.peek()
    }

    private fun checkBreakpoint(): Boolean {
        val frame = currentFrame()
        for (location in frame.locations) {

            // ignore the last line (continue)
            if (location.lineBegin == _lastResumeLine) {
                _lastResumeLine = 0
                return false
            }

            // check breakpoints
            if (breakpoints.containsKey(location.lineBegin)) {
                frame.markCurrentLoc(location)
                return true
            }
        }

        return false
    }

    companion object {
        fun create(): IaDebugger {
            return AppFactory.createDebugger()
        }

        fun GSysExpr.getName(): String {
            return when (this) {
                is GSysProcCallExpr -> "create"
                is GSysDeclRefExpr -> "assign"
                is GSysBinOpExpr -> this.opType.toString().lowercase()
                is GSysRestrictExpr -> "restrict"
                is GSysPruneExpr -> "prune"
                else -> this.javaClass.name
            }
        }
    }
}