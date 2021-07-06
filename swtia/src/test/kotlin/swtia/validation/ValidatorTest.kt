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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import swtia.ia.IaPackage
import swtia.tests.IaInjectorProvider
import swtia.util.TestHelper

@ExtendWith(InjectionExtension::class)
@InjectWith(IaInjectorProvider::class)
class ValidatorTest {
    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var validationTestHelper: ValidationTestHelper

    @ParameterizedTest
    @ValueSource(
        strings = [
            // general declaration
            "actions { i, o } proc P1 {} init {}",
            "actions { i } proc P1 {} init {}",
            "actions { i } var i: int var b: bool proc P1 {} init {}",
            "actions { i } type C { C1, C2} var c1: C proc P1 {} init {}",

            // decl
            "actions { i } var i: int proc P1 {} init {}",
            "actions { i } var i: int proc P1 {} init {}",
            "actions { i } var t1: bool var t2: bool proc P1 {} init {}",

            // type
            "actions { i } type Coin { C1, C2, C3} proc P1 {} init {}",
            "actions { i } type Coin { C1, C2 } var c: Coin proc P1 {} init {}",

            // action expr
            "actions { i } proc P1 { act { i? } i? } init {}",
            "actions { i, o } proc P1 { act { i?, o! } i? o! } init {}",
            "actions { i, o, h } proc P1 { act { i?, o!, h! } i? o! h! tau } init {}",

            // control flow expr
            "actions { i } proc P1 { if true {} } init {}",
            "actions { i } proc P1 { act { i? } if i? {} } init {}",
            "actions { i } proc P1 { if (false) {} else {} } init {}",
            "actions { i } proc P1 { skip error } init {}",
            "actions { i } proc P1 { act { i? } loop { i? } } init {}",
            "actions { i } proc P1 { act { i? } while { i? -> tau } } init {}",
            "actions { i } proc P1 { act { i? } while { i? -> continue tau -> break } } init {}",
            "actions { i } proc P1 { act { i? } case { i? -> skip tau -> error } } init {}",
            "actions { i } proc P1 { { { { { } } } } } init {}",
            "actions { i } proc P1 { label l1: goto l1 } init {}",

            // arithmetic, logical, relational, equality
            "actions { i } proc P1 { true && false } init {}",
            "actions { i } proc P1 { true || false } init {}",
            "actions { i } proc P1 { ~false && true } init {}",
            "actions { i } var a: int var b: int var c: int proc P1 { a + (b + c - 3 + 4) > 3 } init {}",
            "actions { i } var a: int proc P1 { a % (1 / 2 - 3 * 4) == 1 } init {}",
            "actions { i } proc P1 { 3 + 4 == 5 && true || ~false } init {}",
            "actions { i } var a: int var b: int var c: int proc P1 { a > b && a + 5 == c } init {}",
            "actions { i } var a: int var b: int var c: int proc P1 { a >= b && 3 <= 4 || 1 == a || -1 < 2 || 2 > 3 } init {}",
            "actions { i } type Coin { C1, C2} proc P1 { C2 == C1 || C2 != C1 } init {}",

            // expr stmt
            "actions { i } proc P1 { true } init {}",
            "actions { i } proc P1 { -1 < 0 } init {}",
            "actions { i } type Coin { C1, C2} proc P1 { C2 == C1 } init {}",
            "actions { i } var v: int proc P1 { act { i? } i? guarantee(v' >= v) } init {}",

            // operation
            "actions { i } proc P1 {} proc P2 {} init { sys s1 = P1() sys s2 = P2() sys p3 = product(s1, s2) }",
            "actions { i } proc P1 {} proc P2 {} init { sys s1 = P1() sys s2 = P2() sys p3 = composition(s1, s2) }",
            "actions { i } proc P1 {} proc P2 {} init { sys s1 = P1() sys s2 = P2() refinement(s1, s2) }",
            "actions { i } proc P { tau } init { prune(P()) }",
            "actions { i } proc P { tau } proc Q { tau } init { prune(product(P(), Q())) }",
            "actions { i } proc P { tau } init { restrict(P(), i) }",

            // no action
            "proc p {} init {}",

            // action
            "actions { snd, rec, internal } proc Cell(in?, out!) { in? out! } init { sys s1 = Cell(snd?, internal!) sys s2 = Cell(internal?, rec!) }",

            // optional arrow
            "actions { a, b } proc P { act { a?, b! } a? => 1 + 2 + 3 == 6 => b! => true && false } init { P() }",
            "actions { a, b } proc P { act { a?, b! } b! => assume(true) => skip => a?  } init { P() }"
        ]
    )
    fun validateModel(src: String) {
        val model = testHelper.validate(src)
        Assertions.assertNotNull(model)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "var x: int proc p { var x: int loop { var x: int }  } init {}",
            "proc p { var x: int var x: int } init {}",
            "proc p { loop { var x: int var x: int } } init {}",
        ]
    )
    fun errorLocalNotSupported(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GVAR_DECL, null, "local variable is not supported")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { i } proc P1 { a } init {}",
            "actions { i } proc P1 { if a {} } init {}",
        ]
    )
    fun errorNoReferenceVar(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GID_REF_EXPR, "org.eclipse.xtext.diagnostics.Diagnostic.Linking", "Couldn't resolve reference to")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc P1 { o! } init {}",
            "proc P1 { case { o! -> skip } } init {}",
        ]
    )
    fun errorNoReferenceAction(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GACTION_EXPR, "org.eclipse.xtext.diagnostics.Diagnostic.Linking", "Couldn't resolve reference to")
    }

    @Test
    fun errorBreakOutsideLoop() {
        val src = "proc p { break } init {}"
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GJUMP_STMT, null, "cannot 'break' outside of a loop")
    }

    @Test
    fun errorContinueOutsideLoop() {
        val src = "proc p { continue } init {}"
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GJUMP_STMT, null, "cannot 'continue' outside of a loop")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { i } proc P1 { act { i? } ~i? } init {}",
            "actions { i } proc P1 { act { i? }if i? && true {} } init {}",
            "actions { i } proc p { act { i? } tau && i? } init {}",
        ]
    )
    fun errorActionSyncWithOtherExpr(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GACTION_EXPR, null, "action synchronization cannot be used with other expressions")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { i, o } proc P1 { act { i? } if (i?) {} else {} } init {}",
        ]
    )
    fun errorIfActionElseNotAllowed(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GIF_STMT, null, "else branch is not allowed with action on condition")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc p {} proc p {} init {}",
            "proc p {} proc q {} proc p {} init {}"
        ]
    )
    fun errorProcIsDefined(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GPROC, null, "process", "is already defined")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc p {} proc q {} init { sys p1 = p() sys p1 = p() } ",
        ]
    )
    fun errorSysIsDefined(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GSYS_DECL_STMT, null, "system", "is already defined")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "var x: int var x: int proc p {} init {}",
            "var x: int var b: bool var x: int proc p {} init {}",
            "type Drink { Coffee, Coffee } proc p {} init {}",
            "type Drink { Coffee } type T { Coffee, Tea} proc p {} init {}",
            "type T { Coffee, Tea } var Tea: int proc p {} init {}",
        ]
    )
    fun errorVariableDefined(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GCOMMON_VAR, null, "identifier", "is already defined")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions {i, o } proc p { act { i? ,o!, i? } } init {}",
            "actions { o } proc p { act { o!, o! } } init {}",
        ]
    )
    fun errorActionDeclaredInProc(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GACTION_REF, null, "action", "is already declared")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions {i, o } proc p { act { i? ,o! } i! } init {}",
            "actions { o } proc p { act { o! } o? } init {}",
        ]
    )
    fun errorInvalidActionSuffix(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GACTION_EXPR, null, "invalid action suffix")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "type C { C1, C2 } var x: int " +
                    "proc m(p1: int, p2: bool, p3: C, p4: bool) { p2 } " +
                    "init { sys s1 = m(x + 10, 4 == 2, C1, C1 == C2) }",
            "var b: bool proc p(p1: bool) { tau } init { sys t0 = p(b) }",
            "var b: bool proc p(p1: bool) { tau } init { sys t0 = p(~b) }"
        ]
    )
    fun sharedVariableNotAllowedInInit(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GSYS_PROC_CALL_EXPR, null, "shared variable is not allowed in process creation")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "var b: bool proc p(p1: bool) { loop { tau => b' } } init { }",
            "var b: bool proc p(p1: bool) { tau => b' } init { }"
        ]
    )
    fun primedVariableInPostOnly(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GID_REF_EXPR, null, "primed variable is only allowed in 'assume' or 'guarantee'")
    }
}