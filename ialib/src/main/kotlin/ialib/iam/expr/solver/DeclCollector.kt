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

package ialib.iam.expr.solver

import ialib.iam.expr.MCustomType
import ialib.iam.expr.MDecl
import ialib.iam.expr.MExpr
import ialib.iam.expr.translator.AbstractExprTranslator

/**
 * collect all var types in the expr
 */
class DeclCollector private constructor(private val expr: MExpr): AbstractExprTranslator<Unit>() {

    companion object {
        fun collect(expr: MExpr): Map<Pair<String, Boolean>, MDecl> {
            return DeclCollector(expr).getDecls()
        }
    }

    private val mapNames = mutableMapOf<Pair<String, Boolean>, MDecl>()

    fun getDecls(): MutableMap<Pair<String, Boolean>, MDecl> {
        // run
        translate(expr)

        // finish
        return mapNames
    }

    override fun translateTypeValue(type: MCustomType, value: String) {
        
    }

    private var counter = 0

    override fun translateDecl(decl: MDecl, isPrime: Boolean) {

        // add original
        mapNames.getOrPut(Pair(decl.name, false), { decl })

        // create temporary
        if (isPrime) {
            mapNames.getOrPut(Pair(decl.name, true), {
                val tmpName = "__z_t${counter++}"
                MDecl(tmpName, decl.dataType.copy())
            })
        }
    }

    override fun createNumConst(num: Int) {
        
    }

    override fun translatePlus(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateMinus(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateMul(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateDiv(lhs: Unit, rhs: Unit) {
        
    }

    override fun createFalse() {
        
    }

    override fun createTrue() {
        
    }

    override fun translateNot(content: Unit) {
        
    }

    override fun translateGroup(content: Unit) {
        
    }

    override fun translateOr(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateImplies(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateAnd(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateExprGt(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateExprGe(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateExprEq(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateExprNeq(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateExprLt(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateExprLe(lhs: Unit, rhs: Unit) {
        
    }

    override fun translateMod(lhs: Unit, rhs: Unit) {

    }

}