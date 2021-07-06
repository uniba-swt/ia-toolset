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

package ialib.core

data class AutomatonAction(val name: String, val actionType: AutomatonActionType) : Comparable<AutomatonAction> {

    fun formatted(): String {
        return when (actionType) {
            AutomatonActionType.Internal -> tauSymbol // tau symbol
            else -> name + actionType.toSuffixString()
        }
    }

    fun isIo(): Boolean = this.actionType == AutomatonActionType.Input || this.actionType == AutomatonActionType.Output

    fun isInput(): Boolean = this.actionType == AutomatonActionType.Input

    fun isInputOrInternal(): Boolean = this.actionType == AutomatonActionType.Input || this.actionType == AutomatonActionType.Internal

    fun isOutputOrInternal(): Boolean = this.actionType == AutomatonActionType.Output || this.actionType == AutomatonActionType.Internal

    override fun toString(): String {
        return name + actionType.toSuffixString()
    }

    override fun compareTo(other: AutomatonAction): Int {
        return name.compareTo(other.name)
    }

    companion object {

        const val tauSymbol = "\uD835\uDF0F"
        fun ofInput(name: String): AutomatonAction {
            return AutomatonAction(name, AutomatonActionType.Input)
        }

        fun ofOutput(name: String): AutomatonAction {
            return AutomatonAction(name, AutomatonActionType.Output)
        }

        fun tau(): AutomatonAction {
            return AutomatonAction("__tau", AutomatonActionType.Internal)
        }
    }
}