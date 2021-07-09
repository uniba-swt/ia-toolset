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

package ialib.mia.scope

import ialib.core.AutomatonAction
import ialib.iam.expr.MActionExpr
import ialib.mia.ModalAutomaton
import ialib.mia.ModalAutomatonBuilder

/**
 * Implement the Hiding and Restriction operator in MIA
 * See Definition 19 (Hiding) and Definition 20 (Restriction)s
 *
 * In short:
 * - input? transition is deleted
 * - output! transition is transformed into tau
 *
 * For IAM: use restrict operator instead (which delete both input and output transitions)
 */
class ModalScopeOperation {

    fun scope(automaton: ModalAutomaton, setArgs: Set<String>): ModalAutomaton {
        // make sure action is available
        val set = automaton.ioActions.map { a -> a.name }.toSet()
        if (setArgs.any { act -> !set.contains(act) }) {
            throw Exception("actions are not used in the sys '${automaton.name}': $setArgs")
        }

        // build
        val builder = ModalAutomatonBuilder(automaton.name, automaton.initState.name)
        for (state in automaton.getIterator()) {
            for (action in state.actionsSequence) {

                // if input? -> delete (mean ignore)
                if (setArgs.contains(action.name) && action.isInput()) {
                    continue
                }

                // if output! -> transform to tau
                var transformToTau = false
                if (setArgs.contains(action.name) && action.isOutput()) {
                    transformToTau = true
                }

                // must
                for (step in state.getMustSteps(action)) {
                    val act = transformActionIfNeeded(step.action, transformToTau)
                    builder.addMustTransition(state.name, act, step.states.map { s -> s.name })
                }

                // may
                for (step in state.getMaySteps(action)) {
                    val act = transformActionIfNeeded(step.action, transformToTau)
                    for (dst in step.states) {
                        builder.addMayTransition(state.name, act, dst.name)
                    }
                }
            }
        }

        return builder.build()
    }

    private fun transformActionIfNeeded(action: MActionExpr, transformToTau: Boolean): MActionExpr {
        if (!transformToTau)
            return action

        return MActionExpr.of(AutomatonAction.tau(), action.location.copy())
    }
}