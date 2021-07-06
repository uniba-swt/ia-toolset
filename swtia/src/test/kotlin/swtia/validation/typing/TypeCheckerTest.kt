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

package swtia.validation.typing

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import swtia.ia.IaPackage
import swtia.tests.IaInjectorProvider
import swtia.util.TestHelper

@ExtendWith(InjectionExtension::class)
@InjectWith(IaInjectorProvider::class)
class TypeCheckerTest {

    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var validationTestHelper: ValidationTestHelper

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { i } proc P1 { act { i? } i? 1 + 2 == 3 true && (1 == 2) } init {}",
            "type Opt { M, N} var o: Opt proc p { tau o == N M != N } init {}",
            "var x: int var y: int proc p { tau (x + y) >= (y - x) (x * y / -1) == 1 (x % 100) == 2} init {}",
            "var x: int var y: int proc p { tau x > y y >= x x < y y <= -1 } init {}",
            "var x: int var y: int proc p { tau x == y y != x 1 == 2 2 != 2 } init {}",
            "var t: bool proc p { tau true == false true != t } init {}",
            "var t: bool proc p { tau true == false true && t false || t ~t } init {}",
            "proc p { if (1 < 2) {  tau } } init {}"
        ]
    )
    fun validType(src: String) {
        testHelper.validate(src)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "var t: bool proc p {  1 && t => tau } init {}",
            "var t: bool proc p {  t || ~1 => tau } init {}",
        ]
    )
    fun exprTypeErrorBoolButIntOnExpr(src: String) {
        val model = testHelper.parseNoErrors(src)
         validationTestHelper.assertError(model, IaPackage.Literals.GEXPR_STMT, null, "expected type 'bool' but actual 'int'")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc p {  1 } init {}",
            "proc p(x: int) { x } init {}",
            "var x: int proc p { x } init {}",
            "proc p { var x: int x } init {}",
        ]
    )
    fun exprStmtMustBeBool(src: String) {
        val model = testHelper.parseNoErrors(src)
        validationTestHelper.assertError(model, IaPackage.Literals.GEXPR_STMT, null, "expected type 'bool' but actual 'int'")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc P1 { if (2) {} } init {}",
            "var x: int proc P1 { if (x) {} } init {}",
        ]
    )
    fun exprTypeErrorBoolButIntOnIfStmt(src: String) {
        val model = testHelper.parseNoErrors(src)
        validationTestHelper.assertError(model, IaPackage.Literals.GIF_STMT, null, "expected type 'bool' but actual 'int'")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc p { tau => 1 == true } init {}",
            "var x: int proc p { tau => x != true } init {}",
            "type E { M, N } proc p { tau => M != true } init {}",
            "type E { M, N } proc p { tau => M == 1 } init {}",
        ]
    )
    fun exprTypeErrorExpectSameType(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GEXPR_STMT, null, "expected same types but actual")
    }
}