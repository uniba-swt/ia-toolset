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

package swtia.parsing

import com.google.inject.Inject
import org.junit.jupiter.api.extension.ExtendWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.InjectWith
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import swtia.tests.IaInjectorProvider
import swtia.util.TestHelper

@ExtendWith(InjectionExtension::class)
@InjectWith(IaInjectorProvider::class)
class ParsingTest {
    @Inject
    private lateinit var testHelper: TestHelper

    @ParameterizedTest
    @ValueSource(
        strings = [
            // general declaration
            "actions { i, o } proc P1 {} init {}",
            "actions { i } proc P1 {} init {}",
            "actions { i } var i: int var b: bool proc P1 {} init {}",
            "actions { i } type C { C1, C2} var c1: C proc P1 {} init {}",
            "actions { i, o } proc P1 { if (i?) {} else {} } init {}",

            // decl
            "actions { i } var i: int proc P1 {} init {}",
            "actions { i } var i: int proc P1 {} init {}",
            "actions { i } var t1: bool var t2: bool proc P1 {} init {}",

            // type
            "actions { i } type Coin { C1, C2, C3} proc P1 {} init {}",
            "actions { i } type Coin { C1, C2 } var c: Coin proc P1 {} init {}",
            "actions { i } type Coin { C1, C2 } var c: Coin proc P1 {} init {}",

            // action expr
            "actions { i } proc P1 { i? } init {}",
            "actions { i, o } proc P1 { i? o! } init {}",
            "actions { i, o, h } proc P1 { i? o! h! tau } init {}",

            // control flow expr
            "actions { i } proc P1 { if true {} } init {}",
            "actions { i } proc P1 { if (i?) {} else {} } init {}",
            "actions { i } proc P1 { skip error } init {}",
            "actions { i } proc P1 { loop { i? } } init {}",
            "actions { i } proc P1 { while { i? -> tau } } init {}",
            "actions { i } proc P1 { case { i? -> continue tau -> break } } init {}",
            "actions { i } proc P1 { { { { { } } } } } init {}",
            "actions { i } proc P1 { label l1: goto l1 } init {}",
            "actions { i } proc P1 { case { o! -> skip } } init {}",

            // arithmetic, logical, relational, equality
            "actions { i } proc P1 { true && false } init {}",
            "actions { i } proc P1 { true || false } init {}",
            "actions { i } proc P1 { ~false && true } init {}",
            "actions { i } proc P1 { a + (b + c - 3 + 4) } init {}",
            "actions { i } proc P1 { a % (b / c - 3 * 4) } init {}",
            "actions { i } proc P1 { 3 + 4 == 5 && true || ~false } init {}",
            "actions { i } proc P1 { a > b && a + 5 == c } init {}",
            "actions { i } proc P1 { a >= b && 3 <= 4 || 1 == a || -1 < 2 || 2 > 3 } init {}",
            "actions { i } type Coin { C1, C2} proc P1 { C2 == C1 || C2 != C1 } init {}",

            // expr stmt
            "actions { i } proc P1 { true } init {}",
            "actions { i } proc P1 { -1 } init {}",
            "actions { i } type Coin { C1, C2} proc P1 { C2 } init {}",
            "actions { i } var v: int proc P1 { v' >= v } init {}",

            // operation
            "actions { i } proc P1 {} proc P2 {} init { sys p3 = product(P1, P2) }",
            "actions { i } proc P1 {} proc P2 {} init { sys p3 = composition(P1, P2) }",
            "actions { i } proc P1 {} proc P2 {} init { sys p3 = refinement(P1, P2) }",
            "actions { i } proc P1 {} proc P2 {} init { sys p3 = prune(P1()) }",
            "actions { i } proc P1 {} proc P2 {} init { sys p3 = restrict(P1(), i) }",

            // minimal
            "proc p {} init {}"
        ]
    )
    fun parseModel(src: String) {
        val model = testHelper.parseNoErrors(src)
        assertNotNull(model)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { } proc P1 {} init {}",
            "proc P1 {}",
            "init {}"
        ]
    )
    fun parseModelFailed(src: String) {
        assertThrows<AssertionError> {
            testHelper.parseNoErrors(src)
        }
    }
}