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

package swtia.transformation.mir

import ialib.iam.expr.MExpr
import ialib.iam.expr.MLocation
import swtia.util.Constants

abstract class IrStmt(val loc: MLocation)

class IrLabelStmt(val name: String, loc: MLocation): IrStmt(loc)

class IrGotoStmt(val label: IrLabelStmt, loc: MLocation): IrStmt(loc)

class IrExprStmt(val expr: MExpr, loc: MLocation): IrStmt(loc)

class IrAssertStmt(val name: String, val expr: MExpr, loc: MLocation): IrStmt(loc) {
    fun format(): String {
        return "${name}(${expr.format()})"
    }

    fun isAssume(): Boolean {
        return name == Constants.NameAssume
    }

    fun isGuarantee(): Boolean {
        return name == Constants.NameGuarantee
    }

    override fun toString(): String {
        return format()
    }
}

class IrErrorSkipStmt(val isError: Boolean, loc: MLocation): IrStmt(loc)

class IrCaseStmt(val arms: List<Pair<MExpr, IrLabelStmt>>, loc: MLocation): IrStmt(loc)

class IrDisjunctiveGotoStmt(val labels: List<IrLabelStmt>, loc: MLocation): IrStmt(loc)