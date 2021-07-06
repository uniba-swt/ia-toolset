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
class IamValidatorTest {
    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var validationTestHelper: ValidationTestHelper

    @ParameterizedTest
    @ValueSource(
        strings = [
            "#iam actions {a,b} proc P { act {a?, b!} tau => a? => b! } init {}",
            "#iam actions {a,b} proc P { act {a?, b!} tau ; a? ; b! } init {}",
            "#iam actions {a,b} proc P { act {a?, b!} tau => a? ; b! } init {}",
        ]
    )
    fun validateModel(src: String) {
        val model = testHelper.validate(src)
        Assertions.assertNotNull(model)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc P { may tau } init {}",
            "#iam actions {a,b} proc P { act {a?, b!} may a? } init {}",
        ]
    )
    fun errorMayActionNotAllowed(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GACTION_EXPR, null, ErrorMessages.mayActionNotAllowed)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc P { tau => tau or tau } init {}",
            "#iam actions {a,b} proc P { act {a?, b!} a? => tau or b! } init {}",
        ]
    )
    fun errorDisjunctiveMustNotAllowed(src: String) {
        validationTestHelper.assertError(testHelper.parseNoErrors(src), IaPackage.Literals.GDISJUNCTIVE_STMT, null, ErrorMessages.disjunctiveMustStatementNotAllowed)
    }
}