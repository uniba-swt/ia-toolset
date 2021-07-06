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

package ialib.core.pruner

/**
 * Maintain a tree of connected elements
 */
class WeightQuickFindUnion {

    /**
     * parent of each node
     */
    private val mapParent = mutableMapOf<String, String>()

    /**
     * number of element of the the tree with the key as rooted node
     */
    private val mapSize = mutableMapOf<String, Int>()

    /**
     * connect 2 element -> same component
     */
    fun union(fst: String, snd: String) {
        val root1 = rootParent(fst)
        val root2 = rootParent(snd)
        if (root1 == root2)
            return

        // check weight
        // Link root of smaller tree to root of larger tree ( so the weight of higher tree does not change)
        if (getSize(root1) < getSize(root2)) {
            mapParent[root1] = root2
            addWeight(root2, getSize(root1))
        } else {
            mapParent[root2] = root1
            addWeight(root1, getSize(root2))
        }
    }

    private fun getSize(id: String): Int {
        return mapSize.getOrPut(id) { 1 }
    }

    private fun addWeight(id: String, addition: Int) {
        mapSize[id] = getSize(id) + addition
    }

    /**
     * check if 2 components are connected
     */
    fun isConnected(fst: String, snd: String): Boolean {
        return rootParent(fst) == rootParent(snd)
    }

    /**
     * find root parent of a node
     */
    private fun rootParent(id: String): String {
        var key = id
        while (key != parent(key)) {
            key = parent(key)
        }
        return key
    }

    /**
     * find direct parent of a node
     */
    private fun parent(id: String): String {
        return mapParent.getOrPut(id) { id }
    }

    override fun toString(): String {
        return mapParent.toString()
    }
}