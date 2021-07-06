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

package ialib.simpleia.refinement.interactive

import ialib.simpleia.simulation.SimState
import ialib.core.simulation.SimAction
import ialib.core.simulation.RefinementType
import ialib.core.simulation.SubSimAction

class ActionSimCommand(val curState: SimState, val dstState: SimState, val action: SimAction) : AbstractSimCommand() {
    override fun getCommandDesc(indent: String): String {
        // other
        val spec = String.format("%s%s%s - %s -> %s", indent, indent, curState.formattedSpec(), action.actionSpec.formattedString(), dstState.formattedSpec())
        val impl = String.format("%s%s%s - %s -> %s", indent, indent, curState.formattedImpl(), action.actionImpl.formattedString(), dstState.formattedImpl())

        // head
        return if (action.edgeType === RefinementType.Spec) {
            val head = buildHead(curState.formattedSpec(), action.actionSpec)
            head + System.lineSeparator() + spec + System.lineSeparator() + impl
        } else {
            val head = buildHead(curState.formattedImpl(), action.actionImpl)
            head + System.lineSeparator() + impl + System.lineSeparator() + spec
        }
    }

    private fun buildHead(formattedSpec: String, action: SubSimAction): String {
        return String.format("'%s' takes %s action '%s'", formattedSpec, action.actionType.toString().lowercase(), action.formattedString())
    }
}