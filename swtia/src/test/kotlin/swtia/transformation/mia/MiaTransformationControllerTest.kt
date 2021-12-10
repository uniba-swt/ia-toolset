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

package swtia.transformation.mia

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
class MiaTransformationControllerTest {

    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var validationTestHelper: ValidationTestHelper

    @Inject
    private lateinit var miaTransformationController: MiaTransformationController

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia proc P { tau tau or tau } init {}",
            "#mia actions { a } proc P { act { a? } a? tau or {} } init {}",
            "#mia actions { a } proc P { act { a? } a? tau tau a? } init {}",
            "#mia actions { a } proc P { act { a? } case { a? -> may tau tau -> a? or may a? } } init {}",
        ]
    )
    fun validTransform(src: String) {
        validationTestHelper.assertNoErrors(testHelper.parseNoErrors(src))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia actions { a } proc P { act { a? } tau or a? } init {}",
            "#mia actions { a } proc P { act { a? } tau or a? or a? } init {}",
            "#mia actions { a } proc P { act { a? } a?  tau or { a? or may a? } } init {}",
        ]
    )
    fun errorDisjunctiveFollowMust(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, "Disjunctive statement has to follow a must action")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia proc P { may tau  tau or tau } init {}",
            "#mia actions { a } proc P { act { a? } may a?  tau or {} } init {}",
        ]
    )
    fun errorDisjunctiveFollowMustButMay(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, "Disjunctive statement has to follow a must action")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#mia proc P { tau  tau or tau or tau } init {}",
            "#mia actions { i, o } proc P { act { i?, o! } i?  may i? or o! or tau } init {}",
            "#mia actions { i, o } proc P { act { i?, o! } i?  may o! or { i?  i? } or tau } init {}",

            // lowlevel
            "#mia actions { i, o } proc P { act { i?, o! } i?  or { lb0, lb1, lb2 } label lb0: i? label lb1: may o! label lb2: tau  } init {}",
        ]
    )
    fun validMultipleOr(src: String) {
        val model = testHelper.parseNoErrors(src)
        validationTestHelper.assertNoErrors(model)
        val ia = miaTransformationController.transform(model).single().automaton

        // make sure init state has 3 dis
        val action = ia.initState.actionsSequence.first()
        val dest = ia.initState.getMustSteps(action).single()
        Assertions.assertEquals(3, dest.states.size)
    }
}