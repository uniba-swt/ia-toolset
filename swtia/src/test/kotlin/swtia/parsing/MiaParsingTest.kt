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
class MiaParsingTest {
    @Inject
    private lateinit var testHelper: TestHelper

    @ParameterizedTest
    @ValueSource(
        strings = [
            // may action
            "#mia proc P { may tau } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} may a? } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} may b! } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} may b! => may a? => tau } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} case { a? -> {} may a? -> {} b! -> tau may b! -> tau } } init {}",

            // disjunctive-must transition
            "#mia actions {a,b} proc P { act {a?, b!} tau => a? or b! } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} tau => b! or b! } init {}",
            "#mia actions {a,b} proc P { act {a?, b!} a? => { may a? => tau } or b! } init {}",

            // IAM
            "#iam actions {a,b} proc P { act {a?, b!} tau => a? => b! } init {}",
        ]
    )
    fun parseModel(src: String) {
        val model = testHelper.parseNoErrors(src)
        assertNotNull(model)
    }
}