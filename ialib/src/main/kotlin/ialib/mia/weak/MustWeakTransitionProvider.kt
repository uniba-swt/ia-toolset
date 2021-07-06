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

package ialib.mia.weak

import ialib.core.AutomatonAction
import ialib.mia.ModalState

class MustWeakTransitionProvider : WeakTransitionProvideraBase<ModalStateCol>() {

    override fun computeEpsilon(src: ModalState): List<ModalStateCol> {
        return mutableListOf<ModalStateCol>().apply {
            add(ModalStateCol(listOf(src)))
            addAll(computeAdditionalEpsilon(src))
        }
    }

    private fun computeAdditionalEpsilon(src: ModalState): List<ModalStateCol> {
        return src.mustInternalActions.map { action ->
            src.getMustSteps(action).map { step ->
                step.states
                    .map { s -> computeEpsilon(s) }
                    .flatten()
                    .flatMap { s -> s.states }
                    .let { states -> ModalStateCol(states) }
            }
        }.flatten()
    }

    override fun computeOutput(src: ModalState, name: String): List<ModalStateCol> {
        val action = AutomatonAction.ofOutput(name)
        val list = mutableListOf<ModalStateCol>()

        // current
        list.addAll(src.getMustSteps(action).map { dest -> ModalStateCol(dest.states) })

        // leading eps
        computeAdditionalEpsilon(src).forEach { dest ->
            val states = dest.states.map { eps ->
                eps.getMustSteps(action)
                    .flatMap { s -> s.states }
            }.flatten()
            if (states.isNotEmpty())
                list.add(ModalStateCol(states))
        }

        // trailing eps
        return list.map { col -> computeTrailingEpsilon(col) }
    }

    override fun computeInput(
        src: ModalState,
        name: String
    ): List<ModalStateCol> {
        val action = AutomatonAction.ofInput(name)
        return src.getMustSteps(action).map { dest ->
            computeTrailingEpsilon(ModalStateCol(dest.states))
        }
    }

    private fun computeTrailingEpsilon(dest: ModalStateCol): ModalStateCol {
        val states = dest.states
            .map { s -> computeEpsilon(s) }
            .flatten()
            .flatMap { s -> s.states }
        return ModalStateCol((states))
    }
}