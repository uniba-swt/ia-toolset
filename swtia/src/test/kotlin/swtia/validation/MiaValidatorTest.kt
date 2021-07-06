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

package swtia.validation

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import swtia.ia.IaPackage
import swtia.tests.IaInjectorProvider
import swtia.util.TestHelper

@ExtendWith(InjectionExtension::class)
@InjectWith(IaInjectorProvider::class)
class MiaValidatorTest {
    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var validationTestHelper: ValidationTestHelper

    @ParameterizedTest
    @ValueSource(
        strings = [
            // may action
            "#mia proc P { may tau } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} may a? } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} may b! } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} may b! => may a? => tau } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} case { a? -> {} may b! -> {} b! -> tau may b! -> tau } } init {}",

            // disjunctive-must transition
            "#mia actions {a,b} proc P { act {a?, b!} tau => a? or b! } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} tau => b! or b! } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} a? => { may a? => tau } or b! } init {}",
        ]
    )
    fun validateModel(src: String) {
        val model = testHelper.validate(src)
        Assertions.assertNotNull(model)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia actions { a } proc P { act { a? } case { a? -> tau a? -> skip } } init {}",
            "#mia actions { a } proc P { act { a? } case { a? -> tau may a? -> skip } } init {}",
            "#mia actions { a } proc P { act { a? } case { a? -> tau or tau a? -> skip } } init {}",
        ]
    )
    fun validateInput(src: String) {
        val model = testHelper.validate(src)
        Assertions.assertNotNull(model)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia actions { a } proc P { act { a? } a? => tau or a? or tau } init {}",
            "#mia actions { a } proc P { act { a? } a? => { tau } or a? or { error } } init {}",
        ]
    )
    fun validateDisjunctive(src: String) {
        val model = testHelper.validate(src)
        Assertions.assertNotNull(model)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia var x: int proc P { } init {}",
            "#mia var b: bool proc P { } init {}",
        ]
    )
    fun errorVariableNotAllowed(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, ErrorMessages.variableNotAllowedInMia)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia proc Q(x: int) {} init {}",
            "#mia proc Q(x: int, y: bool) {} init {}",
            "#mia proc Q(x: int, a?) {} init {}"
        ]
    )
    fun errorVariableNotAllowedInParams(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GPROC, null, ErrorMessages.variableNotAllowedInMia)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia proc P { true } init {}",
            "#mia proc P { true || false => tau } init {}",
            "#mia proc P { tau => assume(true) } init {}",
            "#mia proc P { 1 == 2 => tau => assume(true) } init {}",
            "#mia proc P { 1 <= 2 => tau => assume(3 < 4) } init {}",
            "#mia proc P { if true { tau } else { tau } } init { }",
        ]
    )
    fun errorExpressionNotAllowed(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GSTMT, null, ErrorMessages.expressionNotAllowed)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia proc P { if (true) { tau } else { tau } } init { }",
            "#mia proc P { case { true -> tau 1 + 2 -> {} } } init { }",
            "#mia proc P { while { true -> tau 1 + 2 -> {} } } init { }",
        ]
    )
    fun errorExpressionNotAllowedInExpr(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GEXPR, null, ErrorMessages.expressionNotAllowed)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia type C { C1 } proc P {} init {}",
        ]
    )
    fun errorDataTypeNotAllowed(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, ErrorMessages.dataTypesNotAllowed)
    }
}