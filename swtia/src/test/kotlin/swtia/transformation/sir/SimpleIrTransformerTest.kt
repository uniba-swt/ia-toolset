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

package swtia.transformation.sir

import com.google.inject.Inject
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.resource.SaveOptions
import org.eclipse.xtext.serializer.impl.Serializer
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import swtia.ia.GIfStmt
import swtia.ia.GLoopStmt
import swtia.ia.GWhileStmt
import swtia.tests.IaInjectorProvider
import swtia.util.TestHelper

@ExtendWith(InjectionExtension::class)
@InjectWith(IaInjectorProvider::class)
class SimpleIrTransformerTest {

    @Inject
    private lateinit var testHelper: TestHelper

    @Inject
    private lateinit var simpleIrTransformer: SimpleIrTransformer

    @Inject
    private lateinit var serializer: Serializer

    @ParameterizedTest
    @ValueSource(
        strings = [
            "proc p { if true {} } init {}",
            "proc p { if tau {} } init {}",
            "proc p { if false {} else {} } init {}",
            "proc p { loop {} } init {}",
            "proc p { while { tau -> skip } } init {}",

            // nested
            "proc p { if true { loop {} } } init {}",
            "proc p { if true { loop {} } else { loop {} } } init {}",
            "proc p { loop { if true {} } } init {}",
            "proc p { while { tau -> loop {} } } init {}",

            // special case
            "proc p { if 1 == 2 { } } init {}",
            "proc p { if 1 > 2 { loop {} } } init {}",
            "proc p { if (true && false) { loop {} } } init {}",
        ]
    )
    fun transformValid(src: String) {
        val model = testHelper.parseNoErrors(src)
        val result = simpleIrTransformer.transform(model)

        // make sure valid
        assertNotNull(result)
        assertNotNull(result.eResource())

        // make sure no if, while, loop
        assertEquals(0, EcoreUtil2.getAllContentsOfType(result, GIfStmt::class.java).size)
        assertEquals(0, EcoreUtil2.getAllContentsOfType(result, GLoopStmt::class.java).size)
        assertEquals(0, EcoreUtil2.getAllContentsOfType(result, GWhileStmt::class.java).size)

        // success serializer
        assertTrue(serializer.serialize(result, SaveOptions.newBuilder().format().options).isNotEmpty())
    }
}