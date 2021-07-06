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

import ialib.core.AutomatonAction
import ialib.core.AutomatonBase
import ialib.iam.MemAutomaton
import ialib.iam.MemState
import ialib.mia.ModalAutomaton
import ialib.mia.ModalState
import swtia.sys.iam.SysIa
import swtia.sys.mia.MiaSysIa
import swtia.sys.models.SysIaBase

open class KeyValueRow(val key: String, val value: String, val type: String, val children: List<KeyValueRow>) {

    companion object {

        const val StringType = "string"
        const val BoolType = "bool"
        const val ObjectType = "object"

        private fun <T> ofList(key: String, items: Collection<T>, provider: (T) -> KeyValueRow): KeyValueRow {
            return ofList(key, items.map(provider))
        }

        private fun ofList(key: String, items: List<KeyValueRow>): KeyValueRow {
            val children = items.withIndex().map { (index, item) ->
                KeyValueRow("[$index]", item.value, item.type, item.children)
            }

            val value = "(${children.size}) [${children.joinToString { c -> c.value }}]"
            return KeyValueRow(key, value, ObjectType, children)
        }

        private fun ofObject(key: String, hintValue: String, props: List<KeyValueRow>): KeyValueRow {
            return KeyValueRow(key, hintValue, ObjectType, props)
        }

        private fun ofString(key: String, value: String): KeyValueRow {
            return KeyValueRow(key, value, StringType, emptyList())
        }

        private fun ofBool(key: String, value: Boolean): KeyValueRow {
            return KeyValueRow(key, value.toString(), BoolType, emptyList())
        }

        /**
         * from IA
         */
        fun fromIa(ia: SysIaBase): KeyValueRow {
            val iaType = when (ia) {
                is SysIa -> "Interface Automata for Shared Memory"
                is MiaSysIa -> "Modal Interface Automata"
                else -> error("not supported IA")
            }

            val list = mutableListOf<KeyValueRow>()
            list.add(ofString("name", ia.name))
            when (ia) {
                is SysIa -> updateAdditionalChildren(ia.automaton, list)
                is MiaSysIa -> updateAdditionalChildren(ia.automaton, list)
            }

            return ofObject(ia.name, iaType, list)
        }

        private fun <S: AutomatonBase> updateAdditionalChildren(automaton: S, list: MutableList<KeyValueRow>) {
            // update inputs and outputs
            list.add(ofList("inputs", automaton.inputActions) { at -> at.toKeyValueRow() })
            list.add(ofList("outputs", automaton.outputActions) { at -> at.toKeyValueRow() })

            // child
            when (automaton) {
                is MemAutomaton -> updateChildForIam(automaton, list)
                is ModalAutomaton -> updateChildForMia(automaton, list)
            }
        }

        private fun updateChildForIam(automaton: MemAutomaton, list: MutableList<KeyValueRow>) {
            list.add(ofString("init", automaton.initState.name))
            list.add(ofList("states", automaton.states) { st -> st.toKeyValueRow() })
        }

        private fun MemState.toKeyValueRow(): KeyValueRow {
            return ofObject(this.name, this.name, listOf(
                ofString("name", this.name),
                ofBool("isInitial", this.isInitial),
                getListTransitionsRow(this)
            ))
        }

        private fun getListTransitionsRow(memState: MemState): KeyValueRow {
            val list = mutableListOf<KeyValueRow>()
            for ((action, steps) in memState.mapSteps) {
                for (step in steps) {
                    // create new object
                    val hint = "[${step.preCond.format()}] ${action.formatted()} [${step.postCond.format()}] -> ${step.dstState.name}"
                    list.add(ofObject(step.id, hint, listOf(
                        ofString("preCond", step.preCond.format()),
                        ofString("action", action.formatted()),
                        ofString("postCond", step.postCond.format()),
                        ofString("dst", step.dstState.name)
                    )))
                }
            }
            return ofList("transitions", list)
        }

        private fun updateChildForMia(automaton: ModalAutomaton, list: MutableList<KeyValueRow>) {
            list.add(ofString("init", automaton.initState.name))
            list.add(ofList("states", automaton.states) { st -> st.toKeyValueRow()})
        }

        private fun ModalState.toKeyValueRow(): KeyValueRow {
            return ofObject(this.name, this.name, listOf(
                ofString("name", this.name),
                ofBool("isInitial", this.isInitial),
                getListTransitionsRow(this)
            ))
        }

        private fun getListTransitionsRow(modalState: ModalState): KeyValueRow {
            val list = mutableListOf<KeyValueRow>()

            // must transitions
            for (steps in modalState.getAllMustSteps()) {
                for (step in steps) {
                    val action = step.action.action
                    if (step.states.size == 1) {
                        val dst = step.states.first()
                        val fmt = "(must) ${action.formatted()} -> ${dst.name}"
                        list.add(ofObject(step.ids, fmt, listOf(
                            ofString("action", action.formatted()),
                            ofString("dst", dst.name),
                        )))
                    } else if (step.states.size > 1){
                        // disjunctive must-transition
                        val dstListObj = ofList("disjunctive", step.states.map { dst ->
                            ofString("dst", dst.name)
                        })
                        val fmt = "(must) ${action.formatted()} -> [${dstListObj.children.joinToString { d -> d.value }}]"
                        list.add(ofObject(step.ids, fmt, listOf(
                            ofString("action", action.formatted()),
                            dstListObj
                        )))
                    }
                }
            }

            // may transitions
            for (steps in modalState.getAllMaySteps()) {
                for (step in steps) {
                    val action = step.action.action
                    // single dest
                    val dst = step.single
                    val fmt = "(may) ${action.formatted()} --> ${dst.name}"
                    list.add(ofObject(step.ids, fmt, listOf(
                        ofString("action", action.formatted()),
                        ofString("dst", dst.name),
                    )))
                }
            }

            return ofList("transitions", list)
        }

        private fun AutomatonAction.toKeyValueRow(): KeyValueRow {
            return ofObject(this.name, this.formatted(), listOf(
                ofString("name", this.name),
                ofString("actionType", this.actionType.toString()),
                ofString("suffix", this.actionType.toSuffixString()),
            ))
        }
    }
}
