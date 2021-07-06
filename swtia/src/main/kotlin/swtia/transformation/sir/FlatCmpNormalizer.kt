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

package swtia.transformation.sir

import org.eclipse.xtext.EcoreUtil2
import swtia.ia.*
import swtia.transformation.ModelFactory

/**
 * alt all compound statements
 *
 * Move all local variables to root state
 * flat all statements
 */
class FlatCmpNormalizer: ProcIrNormalizer() {

    override fun normalize(proc: GProc) {
        // first pass: rename all variables
        renameAllDecls(proc)

        // second pass:
        // move all declarations to top
        // move all cmp to root level of proc
        val decls = mutableListOf<GCommonVar>()
        val stmts = mutableListOf<GStmt>()
        updateToList(proc.body, stmts, decls)

        // create new proc
        val body = ModelFactory.factory.createGCmpStmtBody().also {
            it.decls.addAll(decls)
            it.stmts.addAll(stmts)
        }
        EcoreUtil2.replace(proc.body, body)
    }


    private fun renameAllDecls(proc: GProc) {
        val decls = EcoreUtil2.getAllContentsOfType(proc, GVarDecl::class.java)
        for (decl in decls) {
            decl.name = newTemp(decl.name)
        }
    }

    private fun updateToList(cmpStmtBody: GCmpStmtBody, stmts: MutableList<GStmt>, decls: MutableList<GCommonVar>) {
        decls.addAll(cmpStmtBody.decls)
        for (tmp in cmpStmtBody.stmts) {
            if (tmp is GCmpStmt) {
                updateToList(tmp.body, stmts, decls)
            } else {
                stmts.add(tmp)
            }
        }
    }

    private var tempCounter = 0
    private fun newTemp(name: String): String {
        return "_t${tempCounter++}_$name"
    }
}