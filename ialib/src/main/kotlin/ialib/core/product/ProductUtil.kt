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

package ialib.core.product

import ialib.core.AutomatonAction
import ialib.core.AutomatonBase

object ProductUtil {
    fun computeSharedIOActions(in1: Set<AutomatonAction>, out1: Set<AutomatonAction>, in2: Set<AutomatonAction>, out2: Set<AutomatonAction>): Set<String> {
        val set: MutableSet<String> = HashSet()
        for (act in in1) {
            if (out2.contains(AutomatonAction.ofOutput(act.name)))
                set.add(act.name)
        }
        for (act in out1) {
            if (in2.contains(AutomatonAction.ofInput(act.name)))
                set.add(act.name)
        }
        return set
    }

    fun isComposable(
        in1: Set<AutomatonAction>,
        out1: Set<AutomatonAction>,
        internal1: Set<AutomatonAction>,
        in2: Set<AutomatonAction>,
        out2: Set<AutomatonAction>,
        internal2: Set<AutomatonAction>,
    ): Boolean {
        if (in1.any { o -> in2.contains(o) })
            return false

        if (out1.any { o -> out2.contains(o) })
            return false

        if (internal1.any { a -> isContains(a.name, in2, out2, internal2) })
            return false

        if (internal2.any { a -> isContains(a.name, in1, out1, internal1) })
            return false

        return true
    }

    private fun isContains(
        name: String,
        inSet: Set<AutomatonAction>,
        outSet: Set<AutomatonAction>,
        internalSet: Set<AutomatonAction>,
    ): Boolean {
        if (inSet.contains(AutomatonAction.ofInput(name)))
            return true

        if (outSet.contains(AutomatonAction.ofOutput(name)))
            return true

        return internalSet.contains(AutomatonAction.tau())
    }

    fun formatNotComposableMessage(ia1: AutomatonBase, ia2: AutomatonBase): String {
        val set1 = (ia1.inputActions + ia1.outputActions).joinToString { a -> a.formatted() }
        val set2 = (ia2.inputActions + ia2.outputActions).joinToString { a -> a.formatted() }
        return "Automata are not composable: '${ia1.name}' and '${ia2.name}' ($set1 -- $set2)"
    }
}