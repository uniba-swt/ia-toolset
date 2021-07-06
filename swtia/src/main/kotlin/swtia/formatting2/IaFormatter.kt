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

package swtia.formatting2

import org.eclipse.xtext.formatting2.AbstractJavaFormatter
import org.eclipse.xtext.formatting2.IFormattableDocument
import org.eclipse.xtext.formatting2.IHiddenRegionFormatter
import swtia.ia.*

class IaFormatter : AbstractJavaFormatter() {
    fun format(gModel: GModel, doc: IFormattableDocument) {
        for (gCustomType in gModel.dataTypes) {
            doc.append(gCustomType) { obj: IHiddenRegionFormatter -> obj.newLine() }
            doc.format(gCustomType)
        }
        for (gVarDecl in gModel.decls) {
            doc.append(gVarDecl) { obj: IHiddenRegionFormatter -> obj.newLine() }
            doc.format(gVarDecl)
        }
        for (gProc in gModel.procs) {
            doc.prepend(gProc) { obj: IHiddenRegionFormatter -> obj.newLine() }
            format(gProc, doc)
        }
        doc.prepend(gModel.init) { obj: IHiddenRegionFormatter -> obj.newLine() }
        format(gModel.init, doc)
    }

    fun format(proc: GProc, doc: IFormattableDocument) {
        doc.prepend(textRegionExtensions.regionFor(proc)
            .keyword("act")) { obj: IHiddenRegionFormatter -> obj.newLine() }
        doc.surround(textRegionExtensions.regionFor(proc)
            .keyword("act")) { obj: IHiddenRegionFormatter -> obj.indent() }
        doc.prepend(proc.body) { obj: IHiddenRegionFormatter -> obj.newLine() }
        format(proc.body, doc)
    }

    fun format(initSys: GModelInit, doc: IFormattableDocument) {
        doc.append(textRegionExtensions.regionFor(initSys)
            .keyword("{")) { obj: IHiddenRegionFormatter -> obj.newLine() }
        doc.prepend(textRegionExtensions.regionFor(initSys)
            .keyword("}")) { obj: IHiddenRegionFormatter -> obj.newLine() }
        for (item in initSys.items) {
            doc.surround(item) { obj: IHiddenRegionFormatter -> obj.indent() }
            doc.append(item) { obj: IHiddenRegionFormatter -> obj.newLine() }
        }
    }

    fun format(body: GCmpStmtBody, doc: IFormattableDocument) {
        for (decl in body.decls) {
            doc.surround(decl) { obj: IHiddenRegionFormatter -> obj.indent() }
            doc.append(decl) { obj: IHiddenRegionFormatter -> obj.newLine() }
        }
        for (child in body.stmts) {
            doc.surround(child) { obj: IHiddenRegionFormatter -> obj.indent() }
            doc.append(child) { obj: IHiddenRegionFormatter -> obj.newLine() }
            format(child, doc)
        }
    }

    fun format(stmt: GStmt?, doc: IFormattableDocument) {
        doc.append(textRegionExtensions.regionFor(stmt).keyword("{")) { obj: IHiddenRegionFormatter -> obj.newLine() }
        doc.prepend(textRegionExtensions.regionFor(stmt).keyword("}")) { obj: IHiddenRegionFormatter -> obj.newLine() }
        if (stmt is GCmpStmt) {
            format(stmt.body, doc)
        } else if (stmt is GCaseStmt) {
            format(stmt.caseBody, doc)
        } else if (stmt is GWhileStmt) {
            format(stmt.caseBody, doc)
        }
    }

    fun format(body: GCaseStmtBody, doc: IFormattableDocument) {
        for (branch in body.branches) {
            doc.surround(branch) { obj: IHiddenRegionFormatter -> obj.indent() }
            doc.append(branch) { obj: IHiddenRegionFormatter -> obj.newLine() }
            format(branch.stmt, doc)
        }
    }
}