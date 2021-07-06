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

package ialib.bes

class CollectionBesFormula(private val opType: OpType, val forms: List<BesFormulaBase>) : BesFormulaBase() {
    enum class OpType {
        And, Or
    }

    fun size(): Int {
        return forms.size
    }

    override fun toTextBes(parent: BesFormulaBase?): String {
        if (forms.isEmpty()) return "true"

        if (forms.size == 1) return forms[0].toTextBes(parent)

        val sym = if (opType == OpType.And) " && " else " || "
        val res = forms.map { f: BesFormulaBase -> f.toTextBes(this) }.sorted().joinToString(sym)
        return if (parent != null) {
            "($res)"
        } else res
    }
}