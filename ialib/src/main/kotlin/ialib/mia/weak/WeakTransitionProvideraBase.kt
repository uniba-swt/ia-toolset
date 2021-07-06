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

import ialib.mia.ModalState

abstract class WeakTransitionProvideraBase<T> {
    private val map: MutableMap<String, MutableMap<WeakAction, List<T>>> = mutableMapOf()

    fun getWeakTrans(src: ModalState, weakAction: WeakAction): List<T> {
        if (!map.containsKey(src.name)) {
            map[src.name] = mutableMapOf()
        }

        val mapTransitions = map[src.name] ?: return emptyList()
        if (!mapTransitions.containsKey(weakAction)) {
            // compute if not exist
            mapTransitions[weakAction] = when (weakAction.type) {
                WeakAction.Type.Epsilon -> computeEpsilon(src)
                WeakAction.Type.Output -> computeOutput(src, weakAction.name!!)
                WeakAction.Type.Input -> computeInput(src, weakAction.name!!)
            }
        }

        // return
        return mapTransitions[weakAction] ?: emptyList()
    }

    protected abstract fun computeInput(src: ModalState, name: String): List<T>

    protected abstract fun computeEpsilon(src: ModalState): List<T>

    protected abstract fun computeOutput(src: ModalState, name: String): List<T>
}