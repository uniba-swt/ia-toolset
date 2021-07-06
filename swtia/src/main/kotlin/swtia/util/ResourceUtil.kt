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

package swtia.util

import ialib.core.AutomatonAction
import ialib.core.AutomatonActionType
import ialib.iam.expr.MLocation
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.nodemodel.INode
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import swtia.ia.*

object ResourceUtil {
    fun Resource.getRootModel(): GModel? {
        val obj = this.contents.firstOrNull()
        return if (obj is GModel) { obj } else null
    }

    fun factory(): IaFactory {
        return IaFactory.eINSTANCE
    }

    fun GModel.isIam(): Boolean {
        return this.iaType == null || this.iaType == "iam"
    }

    fun GModel.isMia(): Boolean {
        return this.iaType == "mia"
    }

    fun GExpr.unfoldExpr(): GExpr {
        if (this is GParenthesizedExpr) {
            return this.expr.unfoldExpr()
        }

        return this
    }

    fun EObject.findCurrentProc(): GProc {
        val parent = this.eContainer()
        if (parent == null || parent is GModel) {
            error("object is not part of GProc")
        }

        if (parent is GProc) {
            return parent
        }

        return parent.findCurrentProc()
    }

    fun EObject.getSysStmt(): GSysStmt? {
        val parent = this.eContainer()
        if (parent == null || parent is GModel) {
            return null
        }

        if (parent is GSysStmt) {
            return parent
        }

        return parent.getSysStmt()
    }

    fun EObject.getModel(): GModel {
        return this.eResource().getRootModel()!!
    }

    fun EObject.findSharedVarIdRef(): GIdRefExpr? {

        if (this is GIdRefExpr && this.ref.isGlobalVariable())
            return this

        if (this.eContents() != null) {
            for (eContent in this.eContents()) {
                val child = eContent.findSharedVarIdRef()
                if (child != null) {
                    return child
                }
            }
        }

        return null
    }

    fun GActionExpr.isInput(): Boolean {
        return !this.isTau && this.suffix == GActionSuffix.INPUT
    }

    fun GActionExpr.isOutput(): Boolean {
        return !this.isTau && this.suffix == GActionSuffix.OUTPUT
    }

    fun GActionExpr.isInternal(): Boolean {
        return this.isTau
    }

    fun GCommonVar.isGlobalVariable(): Boolean {
        return this is GVarDecl && this.eContainer() is GModel
    }

    fun EObject.getAssertStmt(): GAssertStmt? {
        val parent = this.eContainer()
        if (parent is GAssertStmt) {
            return parent
        }

        if (parent != null && parent !is GProc && parent !is GModel) {
            return parent.getAssertStmt()
        }

        return null
    }

    fun GProcParam.isActionParam() = this.dataType == null

    fun GProcParam.isDataParam() = this.dataType != null

    fun GActionRef.convertToMAction(): AutomatonAction {
        return when (this.suffix) {
            GActionSuffix.INPUT -> AutomatonAction.ofInput(this.action.name)
            GActionSuffix.OUTPUT -> AutomatonAction.ofOutput(this.action.name)
            else -> throw InternalIaException("unexpected suffix: ${this.suffix}")
        }
    }

    fun GActionExpr.convertToMAction(): AutomatonAction {
        if (this.action !is GProcParam) {
            return when {
                this.isTau -> AutomatonAction.tau()
                this.suffix == GActionSuffix.INPUT -> AutomatonAction.ofInput(this.action.name)
                this.suffix == GActionSuffix.OUTPUT -> AutomatonAction.ofOutput(this.action.name)
                else -> throw InternalIaException("unexpected suffix: ${this.suffix}")
            }
        }

        error("only GAction is supported for $this")
    }

    fun GActionSuffix.toAutomatonActionType(): AutomatonActionType {
        return when (this) {
            GActionSuffix.INPUT -> AutomatonActionType.Input
            GActionSuffix.OUTPUT -> AutomatonActionType.Output
        }
    }

    fun EObject.getLocation(): MLocation {
        val node = getNode()
        if (node != null) {
            val lineCol = NodeModelUtils.getLineAndColumn(node, node.offset)
            val lineColEnd = NodeModelUtils.getLineAndColumn(node, node.offset + node.length)
            return MLocation(node.startLine, node.endLine, lineCol.column, lineColEnd.column, node.offset, node.length)
        }

        return MLocation.empty()
    }

    private fun EObject.getNode(): INode? {
        return NodeModelUtils.findActualNodeFor(this)
    }

    fun GSysStmt.isTemporary(): Boolean {
        return this is GSysDeclStmt && this.name.startsWith("_")
    }

    fun GCustomTypeMem.parentType(): GCustomType {
        return this.eContainer() as GCustomType
    }
}