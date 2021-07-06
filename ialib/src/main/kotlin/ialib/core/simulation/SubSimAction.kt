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

package ialib.core.simulation

import ialib.core.AutomatonAction

/**
 * Mapping from one automaton action
 */
data class SubSimAction(val name: String, val actionType: SimActionType) {

    val isError: Boolean
        get() = actionType === SimActionType.Error

    private val tauSymbol = "\uD835\uDF0F"

    fun formattedString(): String {
        return when(actionType) {
            SimActionType.Internal -> tauSymbol
            else -> name + actionType.toSuffixString()
        }
    }

    fun toAutomatonAction(): AutomatonAction {
        return when(this.actionType) {
            SimActionType.Input -> AutomatonAction.ofInput(name)
            SimActionType.Output -> AutomatonAction.ofOutput(name)
            SimActionType.Epsilon -> AutomatonAction.tau()
            SimActionType.Internal -> AutomatonAction.tau()
            else -> error("cannot convert SubSimAction to AutomatonAction: $this")
        }
    }

    companion object {
        fun createEpsilon(): SubSimAction {
            return SubSimAction("", SimActionType.Epsilon)
        }

        fun create(name: String, actionType: SimActionType): SubSimAction {
            return SubSimAction(name, actionType)
        }

        fun createError(): SubSimAction {
            return SubSimAction("", SimActionType.Error)
        }
    }
}

fun AutomatonAction.toSubSimAction(): SubSimAction {
    return SubSimAction.create(this.name, this.actionType.toSimActionType())
}