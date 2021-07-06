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

import ialib.util.ColorUtil

open class SimAction(val actionSpec: SubSimAction, val actionImpl: SubSimAction, val edgeType: RefinementType) {

    private fun formattedString(): String {
        return String.format("%s_%s", actionSpec.formattedString(), actionImpl.formattedString())
    }

    val name: String
        get() = String.format("%s_%s", actionSpec.name, actionImpl.name)

    val isError: Boolean
        get() = actionSpec.isError || actionImpl.isError

    fun getNfaColor(): String {
        return when {
            isError -> ColorUtil.NfaColorError
            edgeType == RefinementType.Spec -> ColorUtil.NfaColorSpec
            else -> ColorUtil.NfaColorImpl
        }
    }

    override fun toString(): String {
        return formattedString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimAction

        if (actionSpec != other.actionSpec) return false
        if (actionImpl != other.actionImpl) return false
        if (edgeType != other.edgeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = actionSpec.hashCode()
        result = 31 * result + actionImpl.hashCode()
        result = 31 * result + edgeType.hashCode()
        return result
    }
}