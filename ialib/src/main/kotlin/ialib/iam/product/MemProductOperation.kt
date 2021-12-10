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

package ialib.iam.product

import ialib.Ulogger.debugIfEnabled
import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import ialib.core.CoreException
import ialib.core.product.ProductUtil
import ialib.core.product.ProductUtil.computeSharedIOActions
import ialib.core.simulation.AbstractSimTraversor
import ialib.iam.MemAutomaton
import ialib.iam.MemAutomatonBuilder
import ialib.iam.MemStep
import ialib.iam.expr.*
import ialib.iam.expr.solver.DefaultSmtSolver
import org.apache.log4j.Logger
import java.util.function.Consumer

open class MemProductOperation(
    private val solver: DefaultSmtSolver,
    private val ia1: MemAutomaton,
    private val ia2: MemAutomaton,
    private val name: String,
) : AbstractSimTraversor<ProductMemState>() {

    private lateinit var builder: MemAutomatonBuilder

    private lateinit var sharedActNames: Set<String>

    private var errorMsg: String? = null

    open fun build(): MemAutomaton {

        // composable
        if (!isParallelComposable()) {
            throw CoreException(ProductUtil.formatNotComposableMessage(ia1, ia2))
        }

        // prepare
        sharedActNames = computeSharedIOActions(
            ia1.inputActions,
            ia1.outputActions,
            ia2.inputActions,
            ia2.outputActions
        )

        // add all decls
        addDecls(ia1.decls, ia2.decls)

        // start
        val init = ProductMemState(ia1.initState, ia2.initState)
        builder = MemAutomatonBuilder(name, init.cmpMemState.name)

        // actions not shared
        for (action in ia1.ioActions + ia2.ioActions) {
            if (!sharedActNames.contains(action.name)) {
                builder.addActionIfNeeded(action)
            }
        }

        // start
        start(init)

        // finish
        return builder.build()
    }

    /**
     * check composable with internal actions are ignored
     * in IAM, all internal actions are general tau, they are all different
     */
    private fun isParallelComposable() = ProductUtil.isComposable(
        ia1.inputActions,
        ia1.outputActions,
        emptySet(),
        ia2.inputActions,
        ia2.outputActions,
        emptySet()
    )

    override fun processSimState(state: ProductMemState, queueProvider: Consumer<ProductMemState>) {
        logger.debug("Process pair: " + state.id)

        // Automaton 1: Error state
        // TODO

        // Automaton 2: Error state
        // TODO

        // Automaton 1: Shared output action (either synchronised or non-shared)
        for (outputAction in state.st1.outputActions) {
            if (sharedActNames.contains(outputAction.name)) {
                processSharedOutputAction(state, outputAction, queueProvider, true)
            } else {
                processEmptyAction(state, outputAction, true, queueProvider)
            }
        }

        // Automaton 2: Shared output action (either synchronised or non-shared)
        for (outputAction in state.st2.outputActions) {
            if (sharedActNames.contains(outputAction.name)) {
                processSharedOutputAction(state, outputAction, queueProvider, false)
            } else {
                processEmptyAction(state, outputAction, false, queueProvider)
            }
        }

        // Automaton 1: Non-shared input action
        for (inputAction in state.st1.inputActions) {
            if (!sharedActNames.contains(inputAction.name)) {
                processEmptyAction(state, inputAction, true, queueProvider)
            }
        }

        // Automaton 1: Internal action
        for (internalAction in state.st1.internalActions) {
            processEmptyAction(state, internalAction, true, queueProvider)
        }

        // Automaton 2: Non-shared input action
        for (inputAction in state.st2.inputActions) {
            if (!sharedActNames.contains(inputAction.name)) {
                processEmptyAction(state, inputAction, false, queueProvider)
            }
        }

        // Automaton 2: Internal action
        for (internalAction in state.st2.internalActions) {
            processEmptyAction(state, internalAction, false, queueProvider)
        }
    }

    open fun cacheErrorState(state: ProductMemState,
                             action: MActionExpr,
                             outStep: MemStep,
                             inStep: MemStep?,
                             errorMsg: String,
                             isOutInFstState: Boolean
    ) {
        builder.markStateAsError(state.id)
        this.errorMsg = errorMsg
    }

    private fun processEmptyAction(
        state: ProductMemState,
        action: AutomatonAction,
        isFirst: Boolean,
        queueProvider: Consumer<ProductMemState>
    ) {
        // find owner
        val toMoveSt = if (isFirst) state.st1 else state.st2

        // add steps
        for (dstStep in toMoveSt.getDstSteps(action)) {
            // add step
            val prodDst = if (isFirst) ProductMemState(dstStep.dstState, state.st2)
                          else ProductMemState(state.st1, dstStep.dstState)
            val step = addNewStep(state, dstStep.action, dstStep.preCond, dstStep.postCond, prodDst, queueProvider)

            // callback
            val pair = if (isFirst) Pair(dstStep, null) else Pair(null,dstStep)
            onStepAdded(state, step, pair.first, pair.second)
        }
    }

    private fun processSharedOutputAction(
        state: ProductMemState,
        outputAction: AutomatonAction,
        queueProvider: Consumer<ProductMemState>,
        isOutInFstState: Boolean
    ) {
        if (outputAction.actionType !== AutomatonActionType.Output) {
            val outStep = if (isOutInFstState) state.st1.getDstSteps(outputAction).first()
                          else state.st2.getDstSteps(outputAction).first()
            cacheErrorState(state, MActionExpr.of(outputAction, outStep.action.location), outStep, null,
                "Output action $outputAction is not of type output", true)
            return
        }

        // process input
        val inputAction = AutomatonAction.ofInput(outputAction.name)

        // Communication error if there are no input transitions that match the output action
        val st1CommErr = (state.st1.hasAction(outputAction) && !state.st2.hasAction(inputAction))
        val st2CommErr = (state.st2.hasAction(outputAction) && !state.st1.hasAction(inputAction))

        if (st1CommErr || st2CommErr) {
            val outStep = if (st1CommErr) state.st1.getDstSteps(outputAction).first()
                          else state.st2.getDstSteps(outputAction).first()
            val errMsg = "Error state because there are no transitions with $inputAction: ${state.id}"
            cacheErrorState(state, MActionExpr.of(outputAction, outStep.action.location), outStep, null, errMsg, true)
            logger.debug(errMsg)
            return
        }

        // Automaton 1 state output --> Automaton 2 state input
        for (outStep in state.st1.getDstSteps(outputAction)) {
            val inSteps = state.st2.getDstSteps(inputAction)
            val cmpAction = MActionExpr.of(AutomatonAction.tau(), outStep.action.location)
            processSharedStep(state, queueProvider, cmpAction, outStep, inSteps, true)
        }

        // Automaton 2 state output --> Automaton 1 state input
        for (outStep in state.st2.getDstSteps(outputAction)) {
            val inSteps = state.st1.getDstSteps(inputAction)
            val cmpAction = MActionExpr.of(AutomatonAction.tau(), outStep.action.location)
            processSharedStep(state, queueProvider, cmpAction, outStep, inSteps, false)
        }
    }

    private fun processSharedStep(
        state: ProductMemState,
        queueProvider: Consumer<ProductMemState>,
        cmpAction: MActionExpr,
        outStep: MemStep,
        inSteps: List<MemStep>,
        isOutInFstState: Boolean
    ) {
        var atLeastOnePreSat = false
        val preErrors = mutableListOf<Pair<String, MemStep>>()
        for (inStep in inSteps) {

            // validate and add
            val (first, second, msgErr) = validateAndAddStep(state, cmpAction, outStep, inStep, isOutInFstState, queueProvider)

            // flag if result is true
            if (first) {
                atLeastOnePreSat = true
                // there exist a post-condition which is UNSAT -> error state in composition
                if (!second) {
                    cacheErrorState(state, cmpAction, outStep, inStep, msgErr ?: "", isOutInFstState)
                    logger.debug("Error state because the post-condition of a transition is UNSAT: " + state.id)
                }
            } else {
                preErrors.add(Pair(msgErr ?: "", inStep))
            }
        }

        // validate if there is no Pre-condition is SAT
        if (!atLeastOnePreSat && preErrors.isNotEmpty()) {
            val lastPreError = preErrors.first()
            cacheErrorState(state, cmpAction, outStep, lastPreError.second, lastPreError.first, isOutInFstState)
            logger.debug("Error state because the pre-conditions of all transitions are UNSAT: " + state.id)
        }
    }

    /**
     * Validate the the pre and post condition of cross-step, add new step if valid
     *
     * Return tuple of flag:
     * - First item is for pre-condition
     * - Second item is for post-condition
     * @param state
     * @param cmpAction
     * @param outStep
     * @param inStep
     * @param isOutInFstState
     * @param queueProvider
     * @return
     */
    open fun validateAndAddStep(
        state: ProductMemState,
        cmpAction: MActionExpr,
        outStep: MemStep,
        inStep: MemStep,
        isOutInFstState: Boolean,
        queueProvider: Consumer<ProductMemState>
    ): Triple<Boolean, Boolean, String?> {
        logger.debugIfEnabled {
            String.format(
                "validate step: ${state.id} -> ${cmpAction}, in: ${inStep}, out: $outStep",
                state.id,
                cmpAction.toString()
            )
        }

        // pre
        val cmpPreCon = createAndFrm(inStep.preCond, outStep.preCond)
        if (!solver.solve(cmpPreCon)) {
            return Triple(first = false, second = false, third = "The pre-condition conjunction is invalid: $cmpPreCon")
        }

        // post
        val forAllCondition = createAndFrm(cmpPreCon, inStep.postCond)
        if (!solver.solveForAllImplies(forAllCondition, outStep.postCond)) {
            val fmt = "($forAllCondition) ≠> (${outStep.postCond})"
            return Triple(true, second = false, third = "The post-condition implication is invalid: $fmt")
        }

        // add
        val cmpDstSt = if (isOutInFstState) ProductMemState(
            outStep.dstState,
            inStep.dstState
        ) else ProductMemState(inStep.dstState, outStep.dstState)

        val step = addNewStep(state, cmpAction, cmpPreCon, inStep.postCond, cmpDstSt, queueProvider)
        val pair = if (isOutInFstState) Pair(outStep, inStep) else Pair(inStep, outStep)
        onStepAdded(state, step, pair.first, pair.second)
        return Triple(first = true, second = true, null)
    }

    protected open fun onStepAdded(state: ProductMemState, step: MemStep, originStep1: MemStep?, originStep2: MemStep?) {
    }

    private fun addDecls(set1: Set<MDecl>, set2: Set<MDecl>) {
        val names: MutableSet<String> = HashSet()
        val seq = (set1.asSequence() + set2.asSequence())
        for (decl in seq) {
            if (!names.contains(decl.name)) {
                builder.addDecl(decl)
                names.add(decl.name)
            }
        }
    }

    private fun addNewStep(
        src: ProductMemState,
        action: MActionExpr,
        preCond: MExpr,
        postCond: MExpr,
        prodDst: ProductMemState,
        queueProvider: Consumer<ProductMemState>
    ): MemStep {
        logger.debug(String.format("add step: %s - %s -> %s", src.cmpMemState.name, action, prodDst.cmpMemState.name))

        // add state if need
        val step = builder.addTransition(src.cmpMemState.name, prodDst.cmpMemState.name, action, preCond, postCond)

        // new process
        queueProvider.accept(prodDst)
        return step
    }

    private fun createAndFrm(lhs: MExpr, rhs: MExpr): MExpr {
        return MLogicBinExpr(lhs, rhs, MLogicOp.AND)
    }

    companion object {
        private val logger = Logger.getLogger(MemProductOperation::class.java)
    }
}