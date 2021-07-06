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

package swtia.debugger.iad

import org.eclipse.lsp4j.debug.*
import swtia.sys.debugger.IaDebugger

class VariableHandler {

    private val variableStore = VariableStore()

    private val variableNameMapper = VariableNameMapper()

    fun reset() {
        variableStore.clear()
        variableNameMapper.clear()
    }

    fun getVariables(debugger: IaDebugger, requestRef: Int): Array<Variable> {

        // get all variables from scope (ref = 1)
        val list = debugger.getDeclaredAutomata(requestRef)?.map { ia -> KeyValueRow.fromIa(ia) }
        if (list != null) {
            return updateVariables(list).also { items ->
                variableNameMapper.updateNameMapping(items.map { item -> Pair(item.name, item.variablesReference) })
            }
        } else {
            val variable = variableStore.get(requestRef) ?: return emptyArray()
            return updateVariables(variable.children)
        }
    }

    private fun updateVariables(list: List<KeyValueRow>): Array<Variable> {
        return list.map { variableTree ->

            // ref
            val ref = if (variableTree.children.isNotEmpty()) {
                variableStore.create(variableTree)
            } else {
                0
            }

            // map to var
            variableTree.toVariable(ref)
        }.toTypedArray()
    }

    fun evaluate(args: EvaluateArguments): EvaluateResponse {
        // check variableNameMapper
        val ref = variableNameMapper.getRef(args.expression) ?: return EvaluateResponse().also { it.result = "Not found" }
        val row = variableStore.get(ref) ?: return EvaluateResponse().also { it.result = "Not found" }
        return EvaluateResponse().also { res ->
            res.result = row.value
            res.type = row.type
            res.presentationHint = VariablePresentationHint().also { hint -> hint.kind = VariablePresentationHintKind.PROPERTY }
            res.variablesReference = ref
        }
    }

    companion object {
        private fun KeyValueRow.toVariable(ref: Int): Variable {
            return Variable().also { dst ->
                dst.name = this.key
                dst.value = this.value
                dst.type = this.type
                dst.presentationHint = VariablePresentationHint().also { hint ->
                    hint.kind = VariablePresentationHintKind.PROPERTY
                }
                dst.variablesReference = ref
            }
        }
    }
}