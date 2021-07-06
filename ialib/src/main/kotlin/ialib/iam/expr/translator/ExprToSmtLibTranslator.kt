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

package ialib.iam.expr.translator

import ialib.iam.expr.MCustomType
import ialib.iam.expr.MDecl
import ialib.iam.expr.MExpr

/**
 * translate Expr to SMTLib for Z3 sover
 * language reference: https://smtlib.cs.uiowa.edu/papers/smt-lib-reference-v2.6-r2017-07-18.pdf
 */
class ExprToSmtLibTranslator : AbstractExprTranslator<String>() {

    private var mapDecls: Map<Pair<String, Boolean>, MDecl> = mapOf()

    fun buildSmt(mapDecls: Map<Pair<String, Boolean>, MDecl>, expr: MExpr): String {
        this.mapDecls = mapDecls
        return translate(expr)
    }

    override fun createNumConst(num: Int): String {
        return "$num"
    }

    override fun translatePlus(lhs: String, rhs: String): String {
        return "(+ $lhs $rhs)"
    }

    override fun translateMinus(lhs: String, rhs: String): String {
        return "(- $lhs $rhs)"
    }

    override fun translateMul(lhs: String, rhs: String): String {
        return "(* $lhs $rhs)"
    }

    override fun translateDiv(lhs: String, rhs: String): String {
        return "(/ $lhs $rhs)"
    }

    override fun translateMod(lhs: String, rhs: String): String {
        return "(% $lhs $rhs)"
    }

    override fun createFalse(): String {
        return "false"
    }

    override fun createTrue(): String {
        return "true"
    }

    override fun translateNot(content: String): String {
        return "(not $content)"
    }

    override fun translateGroup(content: String): String {
        return content
    }

    override fun translateOr(lhs: String, rhs: String): String {
        return "(or $lhs $rhs)"
    }

    override fun translateImplies(lhs: String, rhs: String): String {
        return "(=> $lhs $rhs)"
    }

    override fun translateAnd(lhs: String, rhs: String): String {
        return "(and $lhs $rhs)"
    }

    override fun translateExprGt(lhs: String, rhs: String): String {
        return "(> $lhs $rhs)"
    }

    override fun translateExprGe(lhs: String, rhs: String): String {
        return "(>= $lhs $rhs)"
    }

    override fun translateExprEq(lhs: String, rhs: String): String {
        return "(= $lhs $rhs)"
    }

    override fun translateExprNeq(lhs: String, rhs: String): String {
         return "(not ${translateExprEq(lhs, rhs)})"
    }

    override fun translateExprLt(lhs: String, rhs: String): String {
        return "(< $lhs $rhs)"
    }

    override fun translateExprLe(lhs: String, rhs: String): String {
        return "(<= $lhs $rhs)"
    }

    /**
     * convert enum member to index and use SMT solver for INT
     * example:
     *   data Coin = C1 | C2
     *   x == data C1
     *   y == data C2
     *
     *   SMT: x == 0
     *   SMT: y == 1
     * */
    override fun translateTypeValue(type: MCustomType, value: String): String {
        val index = type.values.indexOf(value)
        return createNumConst(index)
    }

    override fun translateDecl(decl: MDecl, isPrime: Boolean): String {
        return mapDecls[Pair(decl.name, isPrime)]?.name ?: decl.name
    }
}