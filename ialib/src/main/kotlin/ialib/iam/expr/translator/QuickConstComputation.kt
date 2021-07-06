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

class QuickConstComputation: AbstractExprTranslator<AtomConstValue?>() {
    override fun translateTypeValue(type: MCustomType, value: String): AtomConstValue? {
        return AtomConstValue.ofCustomType(type, value)
    }

    override fun translateDecl(decl: MDecl, isPrime: Boolean): AtomConstValue? {
        return null
    }

    override fun createNumConst(num: Int): AtomConstValue? {
        return AtomConstValue.ofInt(num)
    }

    override fun translatePlus(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofInt(lhsInt + rhsInt)
    }

    override fun translateMinus(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofInt(lhsInt - rhsInt)
    }

    override fun translateMul(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofInt(lhsInt * rhsInt)
    }

    override fun translateDiv(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofInt(lhsInt / rhsInt)
    }

    override fun translateMod(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofInt(lhsInt % rhsInt)
    }

    override fun createFalse(): AtomConstValue? {
        return AtomConstValue.ofBool(false)
    }

    override fun createTrue(): AtomConstValue? {
        return AtomConstValue.ofBool(true)
    }

    override fun translateNot(content: AtomConstValue?): AtomConstValue? {
        val boolVal = content?.getBool() ?: return null
        return AtomConstValue.ofBool(!boolVal)
    }

    override fun translateGroup(content: AtomConstValue?): AtomConstValue? {
        return content
    }

    override fun translateOr(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsBool = lhs?.getBool() ?: return null
        val rhsBool = rhs?.getBool() ?: return null
        return AtomConstValue.ofBool(lhsBool || rhsBool)
    }

    override fun translateImplies(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsBool = lhs?.getBool() ?: return null
        val rhsBool = rhs?.getBool() ?: return null
        return AtomConstValue.ofBool(!lhsBool || rhsBool)
    }

    override fun translateAnd(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsBool = lhs?.getBool() ?: return null
        val rhsBool = rhs?.getBool() ?: return null
        return AtomConstValue.ofBool(lhsBool && rhsBool)
    }

    override fun translateExprGt(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofBool(lhsInt > rhsInt)
    }

    override fun translateExprGe(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofBool(lhsInt >= rhsInt)
    }

    override fun translateExprEq(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        return if (lhs != null && rhs != null) {
            AtomConstValue.ofBool(lhs.equalsTo(rhs))
        } else {
            null
        }
    }

    override fun translateExprNeq(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        return if (lhs != null && rhs != null) {
            AtomConstValue.ofBool(!lhs.equalsTo(rhs))
        } else {
            null
        }
    }

    override fun translateExprLt(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofBool(lhsInt < rhsInt)
    }

    override fun translateExprLe(lhs: AtomConstValue?, rhs: AtomConstValue?): AtomConstValue? {
        val lhsInt = lhs?.getInt() ?: return null
        val rhsInt = rhs?.getInt() ?: return null
        return AtomConstValue.ofBool(lhsInt <= rhsInt)
    }

}

