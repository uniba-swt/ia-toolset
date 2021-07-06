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
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import swtia.debugger.Converter.getSource
import swtia.debugger.Converter.toBreakpointsResponse
import swtia.debugger.Converter.toIaBreakpoints
import swtia.debugger.Converter.toScopeResponse
import swtia.debugger.Converter.toStackTraceResponse
import swtia.debugger.iad.ClientWrapper
import swtia.debugger.iad.VariableHandler
import swtia.debugger.util.Util
import swtia.sys.debugger.IaDebugger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.*


class IaDebugProtocolServer : IDebugProtocolServer {

    private var debugger: IaDebugger? = null
    private val variableHandler = VariableHandler()
    private var client: ClientWrapper? = null
    private var cachedBreakpoints = mutableMapOf<String, SetBreakpointsArguments>()

    /**
     * connect client to debug server adapter
     */
    fun connect(protocolClient: IaDebugProtocolClient) {
        this.client = ClientWrapper(protocolClient).apply {
            initialized()
        }
    }

    /**
     * start underlying debugger
     * must be called from launch() or attach()
     */
    private fun startUnderlyingDebugger(args: MutableMap<String, Any>) {
        val pr = Util.getValue<String>(args, "program") ?: error("missing program path")
        val stopOnEntry = Util.getValue<Boolean>(args, "stopOnEntry") ?: false
        logger.trace("startUnderlyingDebugger: $pr, stopOnEntry: $stopOnEntry")

        // wait for configured
        startProgram(pr, stopOnEntry)
    }

    private fun startProgram(program: String, stopOnEntry: Boolean) {
        this.variableHandler.reset()

        // filter current breakpoints
        val brs = cachedBreakpoints[program]?.toIaBreakpoints() ?: emptyList()

        // run debugger
        debugger = IaDebugger.create().also { db ->
            db.updateBreakpoints(brs)
            db.start(program, stopOnEntry, client)
        }

        // clear
        cachedBreakpoints.clear()
    }

    private fun stopUnderlyingDebugger() {
        logger.trace("stopUnderlyingDebugger")
        debugger = null
        variableHandler.reset()
        cachedBreakpoints.clear()
    }

    private val logger = Util.getLogger(javaClass)

    override fun cancel(args: CancelArguments?): CompletableFuture<Void> {
        logger.trace("cancel: $args")
        return notSupportedEndpoint("cancel")
    }

    override fun runInTerminal(args: RunInTerminalRequestArguments?): CompletableFuture<RunInTerminalResponse> {
        return notSupportedEndpoint("runInTerminal")
    }

    override fun initialize(args: InitializeRequestArguments?): CompletableFuture<Capabilities> {
        logger.info("initialize: $args")
        val capabilities = Capabilities()
        capabilities.supportsConfigurationDoneRequest = true
        capabilities.supportsCompletionsRequest = false
        capabilities.supportsStepInTargetsRequest = false
        capabilities.supportsExceptionInfoRequest = false
        return completedFuture(capabilities)
    }

    override fun configurationDone(args: ConfigurationDoneArguments?): CompletableFuture<Void> {
        logger.info("configurationDone: $args")
        return CompletableFuture<Void>().apply { complete(null) }
    }

    override fun launch(args: MutableMap<String, Any>): CompletableFuture<Void> {
        logger.info("launch: $args")
        return runAsync {
            startUnderlyingDebugger(args)
        }
    }

    override fun attach(args: MutableMap<String, Any>): CompletableFuture<Void> {
        logger.info("attach: $args")
        return runAsync {
            startUnderlyingDebugger(args)
        }
    }

    override fun restart(args: RestartArguments?): CompletableFuture<Void> {
        logger.info("restart: $args")
        return notSupportedEndpoint("restart")
    }

    override fun disconnect(args: DisconnectArguments?): CompletableFuture<Void> {
        logger.info("disconnect: $args")
        return runAsync {
            debugger?.disconnect()
            this.stopUnderlyingDebugger()
        }
    }

    override fun terminate(args: TerminateArguments?): CompletableFuture<Void> {
        logger.info("terminate: $args")
        return notSupportedEndpoint("terminate")
    }

    override fun breakpointLocations(args: BreakpointLocationsArguments?): CompletableFuture<BreakpointLocationsResponse> {
        return notSupportedEndpoint("breakpointLocations")
    }

    override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> {
        val res: SetBreakpointsResponse = debugger?.let { db ->
            if (args.source.path == db.path) {
                db.updateBreakpoints(args.toIaBreakpoints()).toBreakpointsResponse()
            } else {
                SetBreakpointsResponse()
            }
        } ?: cacheBreakpoints(args)

        return completedFuture(res)
    }

    private fun cacheBreakpoints(args: SetBreakpointsArguments): SetBreakpointsResponse {
        // cache
        this.cachedBreakpoints[args.source.path] = args
        return SetBreakpointsResponse().also {
            it.breakpoints = args.breakpoints.map { src ->
                Breakpoint().also { dst ->
                    dst.line = src.line
                    dst.isVerified = true
                }
            }.toTypedArray()
        }
    }

    override fun setFunctionBreakpoints(args: SetFunctionBreakpointsArguments?): CompletableFuture<SetFunctionBreakpointsResponse> {
        return notSupportedEndpoint("setFunctionBreakpoints")
    }

    override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments?): CompletableFuture<Void> {
        logger.trace("setExceptionBreakpoints: $args")
        return runAsync {  }
    }

    override fun dataBreakpointInfo(args: DataBreakpointInfoArguments?): CompletableFuture<DataBreakpointInfoResponse> {
        return notSupportedEndpoint("dataBreakpointInfo")
    }

    override fun setDataBreakpoints(args: SetDataBreakpointsArguments?): CompletableFuture<SetDataBreakpointsResponse> {
        return notSupportedEndpoint("setDataBreakpoints")
    }

    override fun setInstructionBreakpoints(args: SetInstructionBreakpointsArguments?): CompletableFuture<SetInstructionBreakpointsResponse> {
        return notSupportedEndpoint("setInstructionBreakpoints")
    }

    override fun continue_(args: ContinueArguments?): CompletableFuture<ContinueResponse> {
        return supplyAsync {
            this.debugger?.runContinue()
            ContinueResponse().also {
                it.allThreadsContinued = true
            }
        }
    }

    override fun next(args: NextArguments?): CompletableFuture<Void> {
        return runAsync {
            this.debugger?.stepOver()
        }
    }

    override fun stepIn(args: StepInArguments?): CompletableFuture<Void> {
        return runAsync {
            this.debugger?.stepIn()
        }
    }

    override fun stepOut(args: StepOutArguments?): CompletableFuture<Void> {
        return runAsync {
            this.debugger?.stepOut()
        }
    }

    override fun stepBack(args: StepBackArguments?): CompletableFuture<Void> {
        return notSupportedEndpoint("stepBack")
    }

    override fun reverseContinue(args: ReverseContinueArguments?): CompletableFuture<Void> {
        return notSupportedEndpoint("reverseContinue")
    }

    override fun restartFrame(args: RestartFrameArguments?): CompletableFuture<Void> {
        return notSupportedEndpoint("restartFrame")
    }

    override fun goto_(args: GotoArguments?): CompletableFuture<Void> {
        return notSupportedEndpoint("goto_")
    }

    override fun pause(args: PauseArguments): CompletableFuture<Void> {
        logger.trace("pause: $args")
        return runAsync {  }
    }

    override fun stackTrace(args: StackTraceArguments?): CompletableFuture<StackTraceResponse> {
        logger.trace("stackTrace: $args")
        return completedFuture(debugger?.let { db ->
            db.stackFrames.asReversed().toStackTraceResponse(db.getSource())
        })

    }

    override fun scopes(args: ScopesArguments): CompletableFuture<ScopesResponse> {
        logger.trace("scopes: $args")
        return completedFuture(debugger?.getScopes(args.frameId)?.toScopeResponse())
    }

    override fun variables(args: VariablesArguments): CompletableFuture<VariablesResponse> {
        val res = debugger?.let { db ->
            VariablesResponse().apply {
                variables = variableHandler.getVariables(db, args.variablesReference)
            }
        } ?: VariablesResponse()
        return completedFuture(res)
    }

    override fun setVariable(args: SetVariableArguments?): CompletableFuture<SetVariableResponse> {
        logger.trace("setVariable: $args")
        return notSupportedEndpoint("setVariable")
    }

    override fun source(args: SourceArguments?): CompletableFuture<SourceResponse> {
        logger.trace("source: $args")
        return notSupportedEndpoint("source")
    }

    override fun threads(): CompletableFuture<ThreadsResponse> {
        // single thread
        val threads = arrayOf(
            Thread().apply {
                id = threadId
                name = threadName
            }
        )

        return completedFuture(ThreadsResponse().also { res ->
            res.threads = threads
        })
    }

    override fun terminateThreads(args: TerminateThreadsArguments?): CompletableFuture<Void> {
        logger.trace("terminateThreads: $args")
        return notSupportedEndpoint("terminateThreads")
    }

    override fun modules(args: ModulesArguments?): CompletableFuture<ModulesResponse> {
        logger.trace("modules: $args")
        return notSupportedEndpoint("modules")
    }

    override fun loadedSources(args: LoadedSourcesArguments?): CompletableFuture<LoadedSourcesResponse> {
        logger.trace("loadedSources: $args")
        return notSupportedEndpoint("loadedSources")
    }

    override fun evaluate(args: EvaluateArguments): CompletableFuture<EvaluateResponse> {
        logger.trace("evaluate: $args")
        return completedFuture(variableHandler.evaluate(args))
    }

    override fun setExpression(args: SetExpressionArguments?): CompletableFuture<SetExpressionResponse> {
        logger.trace("setExpression: $args")
        return notSupportedEndpoint("setExpression")
    }

    override fun stepInTargets(args: StepInTargetsArguments?): CompletableFuture<StepInTargetsResponse> {
        logger.trace("stepInTargets: $args")
        return notSupportedEndpoint("stepInTargets")
    }

    override fun gotoTargets(args: GotoTargetsArguments?): CompletableFuture<GotoTargetsResponse> {
        logger.trace("gotoTargets: $args")
        return notSupportedEndpoint("gotoTargets")
    }

    override fun completions(args: CompletionsArguments?): CompletableFuture<CompletionsResponse> {
        logger.trace("completions: $args")
        return completedFuture(CompletionsResponse())
    }

    override fun exceptionInfo(args: ExceptionInfoArguments?): CompletableFuture<ExceptionInfoResponse> {
        logger.trace("exceptionInfo: $args")
        return notSupportedEndpoint("exceptionInfo")
    }

    override fun readMemory(args: ReadMemoryArguments?): CompletableFuture<ReadMemoryResponse> {
        logger.trace("readMemory: $args")
        return notSupportedEndpoint("readMemory")
    }

    override fun disassemble(args: DisassembleArguments?): CompletableFuture<DisassembleResponse> {
        logger.trace("disassemble: $args")
        return notSupportedEndpoint("disassemble")
    }

    private fun <T> notSupportedEndpoint(name: String): CompletableFuture<T> {
        error("'$name' endpoint is not supported")
    }

    companion object {
        const val threadId = 1
        const val threadName = "main"
    }
}