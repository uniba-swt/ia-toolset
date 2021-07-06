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

package swtia.util

import com.google.inject.Inject
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.fail
import swtia.ia.GModel
import java.io.File
import java.nio.file.Paths

class TestHelper {
    @Inject
    private lateinit var parseHelper: ParseHelper<GModel>

    @Inject
    private lateinit var validationTestHelper: ValidationTestHelper

    fun parseNoErrors(src: String): GModel {
        val model = parseHelper.parse(src)
        assertNotNull(model)
        val errors = model.eResource().errors
        if (errors.isNotEmpty()) {
            fail { "Unexpected ${errors.size} errors: " + errors.joinToString { e -> e.message } }
        }
        return  model
    }

    fun validate(src: String): GModel {
        val model = parseNoErrors(src)
        validationTestHelper.assertNoErrors(model)
        return model
    }

    fun readFileFromResources(name: String): String {
        return File(ClassLoader.getSystemResource(name).file).readText()
    }

    companion object {
        fun getPathFromResources(name: String): String {
            return Paths.get("src", "test", "resources", name).toAbsolutePath().toString()
        }
    }
}