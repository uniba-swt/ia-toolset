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

package swtia.sys.provider

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
class RefinementBinOpRuntimeProviderTest {

    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var standaloneApp: StandaloneApp

    @ParameterizedTest
    @ValueSource(
        strings = [

            // empty
            "proc P {} proc Q {} init { refinement(Q(), P()) }",
            "proc P {} proc Q {} init { refinement(P(), Q()) }",

            // input
            "actions {a} proc P { act { a? } a? } proc Q { act { a? } a? } init { refinement(Q(), P()) }",
            "actions {a} var x: int proc P { act { a? } case { x > 1 -> a? x < 1 -> a? } } proc Q { act { a? } a? } init { refinement(Q(), P()) }",

            // output
            "actions {b} proc P { act { b! } b! } proc Q { act { b! } b! } init { refinement(Q(), P()) }",
            "actions {b} proc P { act { b! } b! } proc Q { act { b! } tau => tau => tau => b! } init { refinement(Q(), P()) }",

            // internal
            "proc P { tau } proc Q { tau } init { refinement(Q(), P()) }",
            "actions {a, b} var x: int proc P { act { a?, b! } a? => b! => a? } proc Q { act { a?, b! } a? => tau => x > 1 => tau => assume(x' < 1) => b! => a?  } init { refinement(Q(), P()) }",
            "actions {a, b} var x: int proc P { act { a?, b! } a? => x < 1 => tau => assume(x' > 1) => a? } proc Q { act { a?, b! } a? => tau => x > 1 => tau => assume(x' < 1) => tau => a?  } init { refinement(Q(), P()) }",

            // with family
            "actions {a} var x: int proc P { act { a? } a? } proc Q { act { a? } case { x > 1 -> a? x <= 1 -> a? } } init { refinement(Q(), P()) }",
            "actions {a} var x: int proc P { act { a? } a? => guarantee(x' >= 0) } proc Q { act { a? } case { x > 1 -> { a? guarantee(x' == 1) } x <= 1 -> { a? guarantee(x' == 2) } } } init { refinement(Q(), P()) }",
        ]
    )
    fun validRefinement(src: String) {
        val model = testHelper.validate(src)
        val result = standaloneApp.execModel(model)
        assertTrue(result.isSat)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            // missing action
            "actions {a} proc P { act { a? } a? } proc Q { act { a? } } init { refinement(Q(), P()) }",
            "actions {b} proc P { act { b! } } proc Q { act { b! } b! } init { refinement(Q(), P()) }",

            // no input family - pre-cond
            "actions {a} var x: int proc P { act { a? } a? } proc Q { act { a? } case { x > 1 -> a? x < 1 -> a? } } init { refinement(Q(), P()) }",

            // no input family - post-cond
            "actions {a} var x: int proc P { act { a? } a? => guarantee(x' > 0) } proc Q { act { a? } case { x > 1 -> { a? guarantee(x' == 0) } x <= 1 -> a? } } init { refinement(Q(), P()) }",
            "actions {a} var x: int proc P { act { a? } a? => guarantee(x' >= 0) } proc Q { act { a? } case { x > 1 -> { a? guarantee(x' > 0) } x <= 1 -> { a? guarantee(x' < 0) } } } init { refinement(Q(), P()) }",

            // has family -> next state
            "actions {a, b} var x: int proc P { act { a?, b! } a? } proc Q { act { a?, b! } case { x > 1 -> { a? b! } x < 1 -> a? } } init { refinement(Q(), P()) }",

            // no output family - pre-cond
            "actions {b} var x: int proc P { act { b! } case { x > 1 -> b! x < 1 -> b! } } proc Q { act { b! } b! } init { refinement(Q(), P()) }",

            // has family -> next state
            "actions {a, b} var x: int proc P { act { a?, b! } case { x > 1 -> b! x <= 1 -> { b! a? } } } proc Q { act { a?, b! } b! } init { refinement(Q(), P()) }",

            // no output family - post-cond
            "actions {b} var x: int proc P { act { b! } case { x > 1 -> { b! assume(x' > 0) } x <= 1 -> { b! assume(x' == 0) }  } } proc Q { act { b! } b! assume(x' == 0) } init { refinement(Q(), P()) }",

            // internal
            "actions {b} proc P { act { b! } } proc Q { act { b! } tau => tau => tau => b! } init { refinement(Q(), P()) }",
        ]
    )
    fun errorRefinement(src: String) {
        val model = testHelper.validate(src)
        val result = standaloneApp.execModel(model)
        assertFalse(result.isSat)
    }
}