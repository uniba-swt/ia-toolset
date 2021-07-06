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

package swtia.sys.mia

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import swtia.startup.StandaloneApp
import swtia.tests.IaInjectorProvider
import swtia.util.TestHelper

@ExtendWith(InjectionExtension::class)
@InjectWith(IaInjectorProvider::class)
class MiaRuntimeProviderTest {
    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var standaloneApp: StandaloneApp

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia actions { a, b, internal } " +
                    "proc Q { " +
                    "act { a? , b! } " +
                    "while {\n" +
                    "        a? -> b!\n" +
                    "        tau -> a?\n" +
                    "        b! -> tau\n" +
                    "        tau -> b!\n" +
                    "    } " +
                    "} " +
                    "init { " +
                    "sys t0 = Q() " +
                    "sys q1 = restrict(t0, a)" +
                    "} ",
        ]
    )
    fun testRestrict(src: String) {
        val model = testHelper.validate(src)
        val pair = standaloneApp.execModel(model)
        assertTrue(pair.isSat)
        assertNotNull(pair.runtimeData)

        // check q1
        val mia = pair.runtimeData!!.getSys<MiaSysIa>("q1").automaton
        val actions = mia.ioActions.toList()
        assertEquals(1, actions.size)
        assertEquals("b", actions.first().name)

        assertEquals(2, mia.initState.getAllMustSteps().size)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia actions { i, o } proc Q { act { i?, o! }  i? o! tau error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  i? tau tau error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  i? tau o! error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  i? o! o! error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  i? o! error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  i? tau error } init { sys s1 = prune(Q()) }",
            "#mia actions { i } proc Q { act { i? }  i? error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  i? o! or tau error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  i? may o! or tau error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  may i? may o! error } init { sys s1 = prune(Q()) }",
        ]
    )
    fun validPrune(src: String) {
        val pair = standaloneApp.execModel(testHelper.validate(src))
        val ia = pair.runtimeData!!.getSys<MiaSysIa>("s1").automaton
        assertNotNull(ia.initState)
        val action = ia.initState.actionsSequence.single()
        assertEquals("i?", action.formatted())

        // dest -> empty
        assertEquals(0, ia.initState.getStepsSequence(action).first().single.actionsSequence.count())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia actions { o } proc Q { act { o! }  o! error } init { sys s1 = prune(Q()) }",
            "#mia proc Q { tau error } init { sys s1 = prune(Q()) }",
            "#mia proc Q { tau tau error } init { sys s1 = prune(Q()) }",
            "#mia actions { o } proc Q { act { o! }  case { o! -> skip tau -> skip } error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  case { i? -> i? o! -> {} tau -> {} } error } init { sys s1 = prune(Q()) }",
            "#mia actions { i, o } proc Q { act { i?, o! }  case { i? -> i? o! -> tau } error } init { sys s1 = prune(Q()) }"
        ]
    )
    fun pruneIncludeInitState(src: String) {
        val pair = standaloneApp.execModel(testHelper.validate(src))
        assertEquals("invalid result automaton of product (init state is pruned)", pair.errorMsg)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "test_mia_fig1.ia",
        ]
    )
    fun valRefinement(filename: String) {
        val path = TestHelper.getPathFromResources(filename)
        val res = standaloneApp.execRuntime(path)
        assertEquals(true, res.isSat)
    }
}