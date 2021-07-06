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

package swtia.scoping

import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EReference
import org.eclipse.xtext.scoping.IScope
import org.eclipse.xtext.scoping.Scopes
import swtia.ia.*
import swtia.util.ResourceUtil.findCurrentProc
import swtia.util.ResourceUtil.getModel
import swtia.util.ResourceUtil.getRootModel
import swtia.util.ResourceUtil.getSysStmt
import swtia.util.ResourceUtil.isActionParam
import swtia.util.ResourceUtil.isDataParam

class ScopeProxy {

    fun getScope(context: EObject, reference: EReference, defaultProvider: (EObject, EReference) -> IScope): IScope {
        // custom type literal
        when (context) {

            // resolve variable ref or custom type member
            is GIdRefExpr -> {
                if (reference == IaPackage.Literals.GID_REF_EXPR__REF) {
                    return getIdRefScope(context)
                }
            }
            is GActionExpr -> {
                if (reference == IaPackage.Literals.GACTION_EXPR__ACTION) {
                    val init = context.getSysStmt()
                    if (init != null) {
                        return Scopes.scopeFor(init.getModel().actions)
                    }

                    val proc = context.findCurrentProc()
                    val result = mutableListOf<GCommonVar>()
                    result.addAll(proc.actionRefs.map { r -> r.action })
                    result.addAll(proc.params.filter { p -> p.isActionParam() })
                    return Scopes.scopeFor(result)
                }
            }
        }

        return defaultProvider(context, reference)
    }

    private fun getIdRefScope(obj: EObject): IScope {

        val model = obj.eResource().getRootModel() ?: return Scopes.scopeFor(emptyList())

        val outerScope =  Scopes.scopeFor(model.decls)

        // inner scope
        val localItems = mutableListOf<GCommonVar>()
        var parent = obj.eContainer()
        while (true) {
            if (parent == null)
                break

            if (parent is GCmpStmtBody) {
                localItems.addAll(parent.decls)
            }
            if (parent is GProc) {
                // add params and break
                localItems.addAll(parent.params.filter { p -> p.isDataParam() })
                break
            }

            parent = parent.eContainer()
        }

        // add custom type members
        localItems.addAll(model.dataTypes.flatMap { d -> d.members })

        return Scopes.scopeFor(localItems, outerScope)
    }
}