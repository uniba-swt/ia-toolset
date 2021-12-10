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

package swtia.transformation.iam

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import swtia.ia.IaPackage
import swtia.tests.IaInjectorProvider
import swtia.util.TestHelper


@ExtendWith(InjectionExtension::class)
@InjectWith(IaInjectorProvider::class)
class IamValidatorTest {

    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var validationTestHelper: ValidationTestHelper

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc P { false tau } init {}",
            "proc P { false && true tau } init {}",
            "proc P { 1 == 2 || 3 > 4 tau } init {}",
            "proc P { 1 + 2 + 3 != 6 tau } init {}",
            "proc P { true && 1 == 2 tau } init {}",
            "proc P { true == false tau } init {}",
            "proc P { 1 == 2 tau } init {}",
            "proc P { 1 != 1 tau } init {}",
            "proc P { true == false tau } init {}",
            "proc P { false != false tau } init {}",
            "proc P { ~true tau } init {}",
            "type C { C1, C2 } proc P { C1 == C2 tau } init {}",
        ]
    )
    fun errorPreConditionNotSat(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, "Pre-condition is not satisfiable at")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc P { tau assume(false) } init {}",
            "proc P { tau assume(false && true) } init {}",
            "proc P { tau assume(1 + 2 + 3 != 6) } init {}",
            "proc P { tau assume(true && 1 == 2) } init {}",
            "proc P { tau assume(true == false) } init {}",
            "proc P { tau assume(1 == 2) } init {}",
            "type C { C1, C2 } proc P { tau assume(C1 == C2) } init {}",
        ]
    )
    fun errorPostConditionNotSat(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, "Post-condition is not satisfiable at")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { a } var x: int proc P { act { a? } x > 0 a? guarantee(x' >= x && x' == 0) } init {}",
            "actions { b } var x: int proc P { act { b! } x > 0 b! assume(x' >= x && x' == 0) } init {}",
            "actions { a } var x: int proc P { act { a? } case { x > 0 -> a? x == 0 -> a? x < 0 -> a? } guarantee(x' > x && x' == 0) } init {}",
            "actions { a } var x: int proc P { act { a? } case { x > 0 -> a? x == 0 -> a? x < 0 -> a? } guarantee(x' > x && x' < 0) } init {}",
            "actions { a } var x: int proc P { act { a? } case { x > 0 -> a? x == 0 -> a? x < 0 -> a? } guarantee(x' < x && x' > 0) } init {}",
            "actions { a } var x: int proc P { act { a? } case { x > 0 -> a? x == 0 -> a? x < 0 -> a? } guarantee(x' < x && x' == 0) } init {}",
            "actions { b } var x: int proc P { act { b! } case { x > 0 -> b! x == 0 -> b! x < 0 -> b! } assume(x' > x && x' == 0) } init {}",
            "actions { b } var x: int proc P { act { b! } case { x > 0 -> b! x == 0 -> b! x < 0 -> b! } assume(x' > x && x' < 0) } init {}",
            "actions { b } var x: int proc P { act { b! } case { x > 0 -> b! x == 0 -> b! x < 0 -> b! } assume(x' < x && x' > 0) } init {}",
            "actions { b } var x: int proc P { act { b! } case { x > 0 -> b! x == 0 -> b! x < 0 -> b! } assume(x' < x && x' == 0) } init {}",
        ]
    )
    fun errorUnsatPostCondWithPrimed(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, "Unsatisfiable post-condition with primed variable at")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { a } var x: int proc P { act { a? } case { x >= 0 -> a? x <= 0 -> a? } } init {}",
            "actions { a } var x: int proc P { act { a? } case { true -> a? x == 0 -> a? } } init {}",
        ]
    )
    fun errorDataDeterministicInput(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, "Data-determinism is required for input")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { a } var x: int proc P { act { a! } case { x >= 0 -> a! x <= 0 -> a! } assume(x' > x) } init {}",
            "actions { a } var x: int proc P { act { a! } case { true -> a! x == 0 -> a! } assume(x' > 1) } init {}",
            "actions { a } var x: int proc P { act { a? } case { x > 0 -> a? x == 0 -> a? x < 0 -> a? } guarantee(x' > x) } init {}",
            "type C { C1, C2 } proc P { C1 == C1 tau } init {}",
            "proc P { 1 == 2 || 5 >= 4 tau } init {}",
            "proc P { 5 > 4 tau } init {}",
            "proc P { 5 <= 5 tau } init {}",
            "proc P { 4 < 5 tau } init {}",
            "proc P { tau assume(true == true) } init {}",
            "proc P { tau assume(true != false) } init {}",
        ]
    )
    fun validIam(src: String) {
        validationTestHelper.assertNoErrors(testHelper.parseNoErrors(src))
    }
}