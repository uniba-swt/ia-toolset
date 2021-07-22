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

package swtia.sys.iam

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
class IamRuntimeProviderTest {

    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var standaloneApp: StandaloneApp

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { a, b, internal } " +
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
        val sysIa = pair.runtimeData!!.getSys<SysIa>("q1")
        assertEquals(2, sysIa.automaton.initState.mapSteps.size)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc P { tau }\n" +
                "proc Q { tau }\n" +
                "init { sys p1 = product(P(), Q()) }",

            "actions { a }\n" +
                    "proc P {\n" +
                    "    act { a? }\n" +
                    "    a?\n" +
                    "}\n" +
                    "proc Q {\n" +
                    "    act { a! }\n" +
                    "    a!\n" +
                    "}\n" +
                    "init { sys p1 = product(P(), Q()) }",

            "actions { a }\n" +
                    "proc P {\n" +
                    "    act { a? }\n" +
                    "    a?\n" +
                    "}\n" +
                    "proc Q {\n" +
                    "    act { a! }\n" +
                    "    a!\n" +
                    "}\n" +
                    "proc R { tau }\n" +
                    "init { sys p1 = product(product(P(), Q()), R()) }"
        ]
    )
    fun validProductInternalOnly(src: String) {
        val model = testHelper.validate(src)
        val pair = standaloneApp.execModel(model)

        // expect composable
        assertTrue(pair.isSat)
        assertNotNull(pair.runtimeData!!.map["p1"])
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { a }\n" +
                    "proc P {\n" +
                    "    act { a? }\n" +
                    "    a?\n" +
                    "}\n" +
                    "proc Q {\n" +
                    "    act { a! }\n" +
                    "    a!\n" +
                    "}\n" +
                    "init { \n" +
                    "    sys p1 = product(P(), Q())\n" +
                    "}"
        ]
    )
    fun validProduct(src: String) {
        val model = testHelper.validate(src)
        val pair = standaloneApp.execModel(model)

        // expect composable
        assertTrue(pair.isSat)
        assertNotNull(pair.runtimeData!!.getSys<SysIa>("p1"))
        assertEquals("__tau", pair.runtimeData!!.getSys<SysIa>("p1").automaton.initState.actionsSequence.joinToString { a -> a.name })
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { a }\n" +
                    "proc P {\n" +
                    "    act { a? }\n" +
                    "    a?\n" +
                    "}\n" +
                    "proc Q {\n" +
                    "    act { a! }\n" +
                    "    a!\n" +
                    "}\n" +
                    "proc R { tau }\n" +
                    "init { \n" +
                    "    sys p1 = product(P(), Q())\n" +
                    "    sys p2 = product(p1, R())\n" +
                    "}"
        ]
    )
    fun validTwoProductRounds(src: String) {
        val model = testHelper.validate(src)
        val pair = standaloneApp.execModel(model)

        // p2
        assertNotNull(pair.runtimeData!!.getSys("p2"))
        assertEquals("__tau", pair.runtimeData!!.getSys<SysIa>("p2").automaton.initState.actionsSequence.joinToString { a -> a.name })
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { i, o } proc Q { act { i?, o! }  case { o! -> {}  i? ->  { o! tau error } } } init { sys s1 = prune(Q()) }"
        ]
    )
    fun validPrune(src: String) {
        val pair = standaloneApp.execModel(testHelper.validate(src))
        val ia = pair.runtimeData!!.getSys<SysIa>("s1").automaton
        assertNotNull(ia.initState)
        assertEquals(1, ia.initState.mapSteps.keys.size)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { i, o } proc Q { act { i?, o! }  i? tau tau error } init { sys s1 = prune(Q()) }",
            "actions { i, o } proc Q { act { i?, o! }  i? tau o! error } init { sys s1 = prune(Q()) }",
            "actions { i, o } proc Q { act { i?, o! }  i? o! o! error } init { sys s1 = prune(Q()) }",
            "actions { i, o } proc Q { act { i?, o! }  i? o! error } init { sys s1 = prune(Q()) }",
            "actions { i, o } proc Q { act { i?, o! }  i? tau error } init { sys s1 = prune(Q()) }",
            "actions { i } proc Q { act { i? }  i? error } init { sys s1 = prune(Q()) }"
        ]
    )
    fun validPruneRemoveAll(src: String) {
        val pair = standaloneApp.execModel(testHelper.validate(src))
        val ia = pair.runtimeData!!.getSys<SysIa>("s1").automaton
        assertNotNull(ia.initState)
        assertEquals(0, ia.initState.mapSteps.keys.size)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { o } proc Q { act { o! }  o! error } init { sys s1 = prune(Q()) }",
            "proc Q { tau error } init { sys s1 = prune(Q()) }",
            "proc Q { tau tau error } init { sys s1 = prune(Q()) }",
            "actions { o } proc Q { act { o! }  case { o! -> skip tau -> skip } error } init { sys s1 = prune(Q()) }",
            "actions { i, o } proc Q { act { i?, o! }  case { i? -> i? o! -> {} tau -> {} } error } init { sys s1 = prune(Q()) }",
            "actions { i, o } proc Q { act { i?, o! }  case { i? -> i? o! -> tau } error } init { sys s1 = prune(Q()) }"
        ]
    )
    fun pruneIncludeInitState(src: String) {
        val pair = standaloneApp.execModel(testHelper.validate(src))
        assertEquals("invalid result automaton of product (init state is pruned)", pair.errorMsg)
    }
}