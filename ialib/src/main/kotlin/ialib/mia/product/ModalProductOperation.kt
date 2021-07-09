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

package ialib.mia.product

import ialib.Ulogger
import ialib.core.AutomatonAction
import ialib.core.CoreException
import ialib.core.product.ProductUtil
import ialib.core.simulation.AbstractSimTraversor
import ialib.iam.expr.MActionExpr
import ialib.mia.ModalAutomaton
import ialib.mia.ModalAutomatonBuilder
import ialib.mia.ModalState
import ialib.mia.ModalStep
import java.util.function.Consumer

/**
 * Implement MIA product, in short:
 * a? x a! => a!
 * a! x a! => error
 * a? x a? => a?
 *
 * Check: https://www.swt-bamberg.de/luettgen/presentations/pdf/Leuven2016.pdf
 */
class ModalProductOperation : AbstractSimTraversor<ProductModalState>() {

    private lateinit var sharedActNames: Set<String>
    private lateinit var builder: ModalAutomatonBuilder

    fun build(ia1: ModalAutomaton, ia2: ModalAutomaton): ModalAutomaton {
        // composable
        if (!isMiaComposable(
                ia1.outputActions,
                ia2.outputActions,
        )) {
            throw CoreException(ProductUtil.formatNotComposableMessage(ia1, ia2))
        }

        // shared action based on name (input/output action only)
        sharedActNames = computeActions(
            ia1.inputActions,
            ia1.outputActions,
            ia2.inputActions,
            ia2.outputActions
        )

        // prepare
        val init = ProductModalState(ia1.initState, ia2.initState)
        builder = ModalAutomatonBuilder("Product_${ia1.name}_${ia2.name}", init.id)
        start(init)

        // finish
        return builder.build()
    }

    override fun processSimState(state: ProductModalState, queueProvider: Consumer<ProductModalState>) {

        internalProcessOutput(state, state.st1, state.st2, true, queueProvider)
        internalProcessOutput(state, state.st2, state.st1, false, queueProvider)

        // cross for input 1 and 2
        for (inputAction in state.st1.inputActions) {

            // input x input => input
            if (sharedActNames.contains(inputAction.name)) {
                // missing matching on input x input doesn't cause error
                processSharedInput(state, state.st1, state.st2, inputAction, queueProvider)
            }

            // missing case is considered in below iterations
        }

        // empty for input 1
        for (inputAction in state.st1.inputActions) {
            if (!sharedActNames.contains(inputAction.name)) {
                val isMayTran = state.st1.mayInputActions.contains(inputAction)
                processEmptyAction(state, isMayTran, inputAction, true, queueProvider)
            }
        }

        // empty for internal 1
        for (internalAction in state.st1.internalActions) {
            val isMayTran = state.st1.mayInternalActions.contains(internalAction)
            processEmptyAction(state, isMayTran, internalAction, true, queueProvider)
        }

        // empty for input 2
        for (inputAction in state.st2.inputActions) {
            if (!sharedActNames.contains(inputAction.name)) {
                val isMayTran = state.st2.mayInputActions.contains(inputAction)
                processEmptyAction(state, isMayTran, inputAction, false, queueProvider)
            }
        }

        // empty for internal 2
        for (internalAction in state.st2.internalActions) {
            val isMayTran = state.st2.mayInternalActions.contains(internalAction)
            processEmptyAction(state, isMayTran, internalAction, false, queueProvider)
        }
    }

    private fun internalProcessOutput(
        state: ProductModalState,
        attack: ModalState,
        defence: ModalState,
        isFirst: Boolean,
        queueProvider: Consumer<ProductModalState>
    ) {
        // cross/empty for output 1
        for (outputAction in attack.outputActions) {

            // output x input => output
            if (sharedActNames.contains(outputAction.name)) {
                if (!processSharedOutput(state, attack, defence, outputAction, queueProvider)) {
                    markErrorState(state.id)
                }
            } else {
                val isMayTran = attack.mayOutputActions.contains(outputAction)
                processEmptyAction(state, isMayTran, outputAction, isFirst, queueProvider)
            }
        }
    }

    private fun markErrorState(stateId: String) {
        builder.markStateAsError(stateId)
    }

    private fun processEmptyAction(
        state: ProductModalState,
        isMayTran: Boolean,
        action: AutomatonAction,
        isFirst: Boolean,
        queueProvider: Consumer<ProductModalState>
    ) {
        // find owner
        val toMoveSt = if (isFirst) state.st1 else state.st2

        if (isMayTran) {
            // may: add steps
            for (modelDst in toMoveSt.getMaySteps(action)) {
                val dst = modelDst.single
                val prodDst = if (isFirst) {
                    ProductModalState(dst, state.st2)
                } else {
                    ProductModalState(state.st1, dst)
                }
                addNewMayStep(state, action, prodDst, queueProvider)
            }
        } else {
            // must: add steps
            for (modelDst in toMoveSt.getMustSteps(action)) {
                val dstStates = modelDst.states.map { dst ->
                    if (isFirst) {
                        ProductModalState(dst, state.st2)
                    } else {
                        ProductModalState(state.st1, dst)
                    }
                }

                addNewMustStep(state, action, dstStates, queueProvider)
            }
        }
    }

    /**
     * attack by input action, require defence by an input action
     * For the case of defence by an output action, it is already covered in the case of output attack
     * input x input => result input
     */
    private fun processSharedInput(
        state: ProductModalState,
        attack: ModalState,
        defence: ModalState,
        input: AutomatonAction,
        queueProvider: Consumer<ProductModalState>
    ): Boolean {
        return processShared(
            state,
            attack,
            defence,
            input,
            AutomatonAction.ofInput(input.name),
            AutomatonAction.ofInput(input.name),
            queueProvider
        )
    }

    /**
     * attack by output action, require defence by an input action
     * output x input => result output
     */
    private fun processSharedOutput(
        state: ProductModalState,
        attack: ModalState,
        defence: ModalState,
        output: AutomatonAction,
        queueProvider: Consumer<ProductModalState>
    ): Boolean {
        return processShared(
            state,
            attack,
            defence,
            output,
            AutomatonAction.ofInput(output.name),
            AutomatonAction.ofOutput(output.name),
            queueProvider
        )
    }

    private fun processShared(
        state: ProductModalState,
        attack: ModalState,
        defence: ModalState,
        attackAct: AutomatonAction,
        defenceAct: AutomatonAction,
        cmpAction: AutomatonAction,
        queueProvider: Consumer<ProductModalState>
    ): Boolean {

        // process must if: must? - must!
        val st1Must = attack.getMustSteps(attackAct)
        if (st1Must.isNotEmpty()) {
            val st2Must = defence.getMustSteps(defenceAct)
            if (st2Must.isNotEmpty()) {
                for (modalDest1 in st1Must) {
                    for (modalDest2 in st2Must) {
                        addNewMustStep(state, cmpAction, modalDest1, modalDest2, queueProvider)
                    }
                }

                // stop
                return true
            }
        }

        // all other cases considered as may - may
        val st1MaySequence = attack.getStepsSequence(attackAct)
        val st2MaySequence = defence.getStepsSequence(defenceAct)
        var matched = false
        for (modalDest1 in st1MaySequence) {
            for (dst1 in modalDest1.states) {
                for (modalDest2 in st2MaySequence) {
                    for (dst2 in modalDest2.states) {
                        addNewMayStep(state, cmpAction, ProductModalState(dst1, dst2), queueProvider)
                        matched = true
                    }
                }
            }
        }

        return matched
    }

    private fun addNewMustStep(
        state: ProductModalState,
        cmpAction: AutomatonAction,
        modalStep1: ModalStep,
        modalStep2: ModalStep,
        queueProvider: Consumer<ProductModalState>
    ) {
        val productDstStates = productModalDest(modalStep1, modalStep2)
        val dstIds = mutableListOf<String>()

        productDstStates.forEach { s ->
            dstIds.add(s.id)
            queueProvider.accept(s)
        }

        val src = state.id
        Ulogger.debug { "add must step: $src - $cmpAction -> $dstIds" }
        builder.addMustTransition(src, MActionExpr.mustOf(cmpAction), dstIds)
    }

    private fun addNewMayStep(
        state: ProductModalState,
        cmpAction: AutomatonAction,
        dstState: ProductModalState,
        queueProvider: Consumer<ProductModalState>
    ) {
        val src = state.id
        val dst = dstState.id
        Ulogger.debug { "add may step: $src - $cmpAction -> $dst" }
        builder.addMayTransition(src, MActionExpr.mayOf(cmpAction), dst)
        queueProvider.accept(dstState)
    }

    private fun addNewMustStep(
        state: ProductModalState,
        cmpAction: AutomatonAction,
        dstStates: Collection<ProductModalState>,
        queueProvider: Consumer<ProductModalState>
    ) {
        val cmpStates = mutableListOf<String>()
        dstStates.forEach { s ->
            queueProvider.accept(s)
            cmpStates.add(s.id)
        }

        builder.addMustTransition(state.id, MActionExpr.mustOf(cmpAction), cmpStates)
    }

    private fun productModalDest(step1: ModalStep, step2: ModalStep): List<ProductModalState> {
        val list = mutableListOf<ProductModalState>()
        for (dst1 in step1.states) {
            for (dst2 in step2.states) {
                list.add(ProductModalState(dst1, dst2))
            }
        }

        return list
    }

    companion object {

        /**
         * MIAs P1, P2 are composable if O1 intersect O2 = empty;
         * Inputs can be presented in both
         */
        private fun isMiaComposable(
            out1: Set<AutomatonAction>,
            out2: Set<AutomatonAction>,
        ): Boolean {
            if (out1.any { o -> out2.contains(o) })
                return false

            return true
        }

        /**
         * Input can be matched by either input or output
         * Output can be matched by input only
         */
        private fun computeActions(
            in1: Set<AutomatonAction>,
            out1: Set<AutomatonAction>,
            in2: Set<AutomatonAction>,
            out2: Set<AutomatonAction>
        ): Set<String> {
            val set: MutableSet<String> = HashSet()
            for (act in in1) {
                if (in2.contains(AutomatonAction.ofInput(act.name)) || out2.contains(AutomatonAction.ofOutput(act.name)))
                    set.add(act.name)
            }

            for (act in out1) {
                if (in2.contains(AutomatonAction.ofInput(act.name)))
                    set.add(act.name)
            }

            return set
        }
    }
}