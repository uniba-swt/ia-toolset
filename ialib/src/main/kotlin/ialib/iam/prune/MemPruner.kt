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

package ialib.iam.prune

import ialib.core.pruner.PrunerBase
import ialib.iam.MemAutomaton
import ialib.iam.MemAutomatonBuilder
import ialib.iam.MemState

class MemPruner(ia: MemAutomaton, private val name: String): PrunerBase<MemAutomaton, MemState>(ia) {

    override fun getAutonomousDstStates(src: MemState): Sequence<MemState> {
        return sequence {
            for ((action, steps) in src.mapSteps) {
                if (action.isOutputOrInternal()) {
                    for (step in steps) {
                        yield(step.dstState)
                    }
                }
            }
        }
    }

    override fun getInitState(): MemState {
        return ia.initState
    }

    override fun rebuildIa(errorStates: Set<String>): MemAutomaton {
        val builder = MemAutomatonBuilder(name, ia.initState.name, ia.decls)

        ia.ioActions.forEach { a -> builder.addActionIfNeeded(a) }

        for (st in ia.getIterator()) {

            // delete outgoing transition from error statate
            if (errorStates.contains(st.name))
                continue
            
            // add transition
            for ((_, steps) in st.mapSteps) {
                for (step in steps) {

                    // delete incoming transitions to error state
                    if (errorStates.contains(step.dstState.name))
                        continue

                    // add
                    builder.addTransition(st.name, step.dstState.name, step.action, step.preCond, step.postCond)
                }
            }
        }

        return builder.build()
    }
}

