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

package swtia.transformation

import com.google.inject.Inject
import org.eclipse.xtext.serializer.impl.Serializer
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import swtia.ia.IaPackage
import swtia.tests.IaInjectorProvider
import swtia.transformation.iam.IamTransformController
import swtia.transformation.iam.cfg.IamCfgDotBuilder
import swtia.util.TestHelper

@ExtendWith(InjectionExtension::class)
@InjectWith(IaInjectorProvider::class)
class TransformationControllerTest {

    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var validationTestHelper: ValidationTestHelper

    @Inject
    private lateinit var controllerIam: IamTransformController

    @Inject
    private lateinit var serializer: Serializer

    @Test
    fun testSampleTransformFile() {
        val src = testHelper.readFileFromResources("sample_transform.ia")
        val model = testHelper.validate(src)
        assertNotNull(model)

        // transform to IR
        val listIa = controllerIam.transformModel(model)
        assertEquals(5, listIa.size)
        for (sysIa in listIa) {
            assertNotNull(sysIa)
        }
    }

    @Test
    fun replaceProcArgByLiteral() {
        val src = "type C { C1, C2 } var x: int " +
                "proc m(p1: int, p2: bool, p3: C, p4: bool, p5: bool) { p2 } " +
                "init { sys s1 = m(10 + 1, 4 == 2, C1 , C1 == C2, C1 != C2) }"
        val model = testHelper.validate(src)
        assertNotNull(model)

        // make sure new IR contain
        val text = serializer.serialize(model)
        val expected = "sys s1 = m( 11 , false , C1 , false , true )"
        assertTrue(text.contains(expected)) { "expected stmt to be contained \n '$expected' \nby\n'$text' " }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc m {} init { sys s1 = m() }",
            "proc m { loop {} tau } init { sys s1 = m() }",
            "proc m { while { true -> skip } tau } init { sys s1 = m() }",
        ]
    )
    fun validNoAction(src: String) {
        testHelper.validate(src)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "actions { i } proc m { act { i? } i? assume(true)  } init { }",
            "actions { i } proc m { act { i? } i? i? assume(true)  } init { }",
            "actions { o } proc m { act { o! } o! guarantee(true)  } init { }",
        ]
    )
    fun errorInvalidPostCondition(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, "invalid post condition")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc m { assume(true) tau } init { }",
            "proc m { guarantee(true) tau } init { }",
        ]
    )
    fun errorNoActionPostCondition(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GMODEL, null, "no action found for post condition")
    }
}