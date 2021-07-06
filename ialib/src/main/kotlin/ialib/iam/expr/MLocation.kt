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

package ialib.iam.expr

/**
 * line and column are both 1-based
 *
 */
data class MLocation(var lineBegin: Int, var lineEnd: Int, var colBegin: Int, var colEnd: Int, val offset: Int, val length: Int) {

    fun toEndOnly(): MLocation {
        return MLocation(lineEnd, 0, colEnd, 0, 0, 0)
    }

    fun isEmpty(): Boolean {
        return lineBegin == 0 && colBegin == 0
    }

    companion object {
        /**
         * for manually created expr
         */
        fun empty() = MLocation(0, 0, 0, 0, 0, 0)

        fun Iterator<MLocation>.mergeLocations(): MLocation {
            var final: MLocation? = null
            for (loc in this) {
                // ignore invalid
                if (loc.lineBegin <= 0 || loc.colBegin <= 0) {
                    continue
                }

                // assign
                if (final == null) {
                    final = MLocation(
                            loc.lineBegin,
                            loc.lineEnd,
                            loc.colBegin,
                            loc.colEnd,
                        0,
                        0
                    )
                    continue
                }

                // begin
                if (loc.lineBegin < final.lineBegin) {
                    final.lineBegin = loc.lineBegin
                    final.colBegin = loc.colBegin
                } else if (loc.lineBegin == final.lineBegin) {
                    final.colBegin = final.colBegin.coerceAtMost(loc.colBegin)
                }

                // end
                if (loc.lineEnd > final.lineEnd) {
                    final.lineEnd = loc.lineEnd
                    final.colEnd = loc.colEnd
                } else if (loc.lineEnd == final.lineEnd) {
                    final.colEnd = final.colEnd.coerceAtLeast(loc.colEnd)
                }
            }

            return final ?: empty()
        }
    }
}