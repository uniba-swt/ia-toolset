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

package swtia.transformation.mia.cfg

import swtia.common.expr.XExprStringUtil.format

object MiaCfgDotBuilder {

    fun toDot(cfg: MiaCfg): String {
        val builder = StringBuilder()
        builder.append("digraph MIA_CFG {\n")

        val iterator = cfg.getIterator()
        for (vertex in iterator) {
            val (vColor: String, vShape: String) = getVertexColorShape(vertex.type)

            // print node
            builder.append("${vertex.name}[label=\"${vertex.name}\",color=$vColor,shape=$vShape]\n")

            // init node
            if (vertex == cfg.init) {
                builder.append("_qi[shape=none,label=\"\"]")
                builder.append("_qi -> ${vertex.name}\n")
            }

            // print edges
            for (edge in cfg.outgoingEdgesOf(vertex)) {
                val (eColor, eStyle, eLabel) = getEdgeColorStyleLabel(edge)
                val dst = cfg.getEdgeDst(edge)
                builder.append("${vertex.name} -> ${dst.name}[label=\"$eLabel\",color=\"$eColor\",style=\"$eStyle\"]\n")
            }
        }

        builder.append("}")
        return builder.toString()
    }

    private fun getVertexColorShape(type: MiaCfgVertexType): Pair<String, String> {
        return when (type) {
            MiaCfgVertexType.Label -> {
                Pair("gray", "oval")
            }
            MiaCfgVertexType.Error -> {
                Pair("red", "rect")
            }
        }
    }

    private fun getEdgeColorStyleLabel(edge: MiaCfgEdge): Triple<String, String, String> {
        when (edge.type) {
            MiaCfgEdgeType.Empty -> {
                return Triple("gray", "solid", "")
            }
            MiaCfgEdgeType.Action -> {
                val action = edge.action!!
                if (action.isMay) {
                    return Triple("blue", "dashed", action.format())
                }

                return Triple("blue", "bold", action.format())
            }
            MiaCfgEdgeType.Disjunctive -> {
                return Triple("cyan4", "solid", "")
            }
        }
    }
}