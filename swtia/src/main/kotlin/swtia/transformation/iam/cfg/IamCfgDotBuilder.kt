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

package swtia.transformation.iam.cfg

import ialib.iam.expr.MActionExpr

object IamCfgDotBuilder {

    fun toDot(cfg: IamCfg): String {
        val builder = StringBuilder()
        builder.append("digraph IAM_CFG {\n")

        val iterator = cfg.getIterator()
        for (vertex in iterator) {
            if (vertex != null) {
                val lb = when (vertex.type) {
                    IamCfgVertexType.Assert -> vertex.assertion?.format() ?: ""
                    else -> vertex.name
                }
                val (color: String, shape: String) = getVertexColorShape(vertex.type)

                // print node
                builder.append("${vertex.name}[label=\"$lb\",color=$color,shape=$shape]\n")

                // init node
                if (vertex == cfg.init) {
                    builder.append("_qi[shape=none,label=\"\"]")
                    builder.append("_qi -> ${vertex.name}\n")
                }
            }

            // edges
            for (edge in cfg.outgoingEdgesOf(vertex)) {
                val lb = when {
                    edge.value != null -> edge.value.format()
                    else -> ""
                }
                val edgeColor = if (edge.value is MActionExpr) "blue" else "gray"
                val dst = cfg.getEdgeDst(edge)
                builder.append("${vertex.name} -> ${dst.name}[label=\"$lb\",color=\"$edgeColor\"]\n")
            }
        }

        builder.append("}")
        return builder.toString()
    }

    private fun getVertexColorShape(type: IamCfgVertexType): Pair<String, String> {
        return when (type) {
            IamCfgVertexType.Assert -> {
                Pair("green", "oval")
            }
            IamCfgVertexType.Label -> {
                Pair("gray", "oval")
            }
            IamCfgVertexType.Error -> {
                Pair("red", "rect")
            }
        }
    }
}