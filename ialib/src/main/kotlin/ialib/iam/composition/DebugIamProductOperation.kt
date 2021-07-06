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

package ialib.iam.composition

import ialib.iam.MemAutomaton
import ialib.iam.MemState
import ialib.iam.MemStep
import ialib.iam.composition.debug.MemProductStackFrame
import ialib.iam.composition.debug.MemProductTraceRecord
import ialib.iam.composition.debug.ShortestTraceProvider
import ialib.iam.debug.DebugSession
import ialib.iam.expr.MActionExpr
import ialib.iam.expr.MLocation
import ialib.iam.expr.solver.DefaultSmtSolver
import ialib.util.EventBus
import ialib.util.ProductBusMessage
import ialib.util.ProductBusType
import java.util.function.Consumer

class DebugIamProductOperation(solver: DefaultSmtSolver, ia1: MemAutomaton, ia2: MemAutomaton, name: String)
    :IamProductOperation(solver, ia1, ia2, name) {

    val frame = MemProductStackFrame(2, "product")

    private val shortestProvider = ShortestTraceProvider()

    override fun build(): MemAutomaton {
        EventBus.publish(ProductBusMessage(this, ProductBusType.WillStart))
        val res = super.build()
        EventBus.publish(ProductBusMessage(this, ProductBusType.Ended))
        return res
    }

    override fun start(initState: ProductMemState) {
        shortestProvider.initId = initState.id
        super.start(initState)
    }

    override fun processSimState(state: ProductMemState, queueProvider: Consumer<ProductMemState>) {
        frame.updateLocations(state.getLocations())
        if (!frame.loc.isEmpty()) {
            EventBus.publish(ProductBusMessage(this, ProductBusType.WillProcessState))
            DebugSession.waitForResumeIfNeeded()
        }

        super.processSimState(state, queueProvider)

        if (!frame.loc.isEmpty()) {
            EventBus.publish(ProductBusMessage(this, ProductBusType.EndedProcessState))
            DebugSession.waitForResumeIfNeeded()
        }
    }

    override fun cacheErrorState(
        state: ProductMemState,
        action: MActionExpr,
        outStep: MemStep,
        inStep: MemStep?,
        errorMsg: String,
        isOutInFstState: Boolean
    ) {
        super.cacheErrorState(state, action, outStep, inStep, errorMsg, isOutInFstState)
        frame.updateLocation(action.location)

        // find shortest trace
        val traces = shortestProvider.findShortestTrace(state) ?: frame.traces.asIterable().toList()

        // add error mem trace
        val pair = if (isOutInFstState) Pair(outStep, inStep) else Pair(inStep, outStep)
        val errorTrace = MemProductTraceRecord(errorMsg, pair.first, pair.second)
        frame.traces.clear()
        for (trace in traces) {
            frame.traces.push(trace)
        }
        frame.traces.push(errorTrace)

        EventBus.publish(ProductBusMessage(this, ProductBusType.ErrorState, errorMsg))

        // wait for resume
        DebugSession.waitForResumeIfNeeded()
    }

    override fun onStepAdded(state: ProductMemState, step: MemStep, originStep1: MemStep?, originStep2: MemStep?) {
        super.onStepAdded(state, step, originStep1, originStep2)
        frame.updateLocation(step.action.location)

        // trace
        val trace = MemProductTraceRecord(step, originStep1, originStep2)
        frame.traces.push(trace)

        // cache to trace
        shortestProvider.addStep(state.id, step.dstState.name, trace)
    }

    companion object {
        fun ProductMemState.getLocations(): List<MLocation> {
            return (this.st1.getLocations() + this.st2.getLocations()).toList()
        }

        private fun MemState.getLocations(): Sequence<MLocation> {
            return this.actionsSequence.asSequence().map { action ->
                this.getDstSteps(action).map { st ->
                    st.action.location
                }
            }.flatten()
        }
    }
}

