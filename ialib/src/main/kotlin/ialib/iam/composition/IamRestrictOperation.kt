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
import ialib.iam.MemAutomatonBuilder

class IamRestrictOperation {

    /**
     * restrict operator
     * - keep tau action
     * - delete input and output action (including transition)
     */
    fun restrict(automaton: MemAutomaton, setArgs: Set<String>): MemAutomaton {
        // make sure action is available
        val set = automaton.ioActions.map { a -> a.name }.toSet()
        if (setArgs.any { act -> !set.contains(act) }) {
            throw Exception("actions are not used in the sys '${automaton.name}': $setArgs")
        }

        // build new
        val builder = MemAutomatonBuilder(automaton.name, automaton.initState.name, automaton.decls)
        for (state in automaton.getIterator()) {
            for ((action, steps) in state.mapSteps) {

                // is input or output -> delete transitions (ignore)
                if (setArgs.contains(action.name) && action.isIo())
                    continue

                // add step
                for (step in steps) {
                    builder.addTransition(state.name, step.dstState.name, step.action, step.preCond, step.postCond)
                }
            }
        }

        // phase 2: remove
        return builder.build()
    }
}

