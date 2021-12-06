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

package ialib.core.refinement

import org.apache.log4j.Logger
import ialib.core.AutomatonAction
import ialib.core.AutomatonBase

object RefinementUtil {
    private val logger = Logger.getLogger(RefinementUtil::class.java)

    /**
     * AutomatonBase
     *
     * ensure
     * specIa.inputs == implIa.inputs
     * &&
     * specIa.outputs == implIa.outputs
     * @param abstractIa IA Spec
     * @param specificIa IA Impl
     * @return true if satisfied, otherwise false
     */
    fun isInputOutputEqual(specificIa: AutomatonBase, abstractIa: AutomatonBase): Boolean {
        return isInputOutputEqual(abstractIa.inputActions, abstractIa.outputActions, specificIa.inputActions, specificIa.outputActions)
    }

    private fun isInputOutputEqual(
        specInputs: Set<AutomatonAction>,
        specOutputs: Set<AutomatonAction>,
        implInputs: Set<AutomatonAction>,
        implOutputs: Set<AutomatonAction>
    ): Boolean {
        if (implInputs != specInputs) {
            logger.debug("Implementation and specification inputs are not equal")
            return false
        }
        if (implOutputs != specOutputs) {
            logger.debug("Implementation and specification outputs are not equal")
            return false
        }
        return true
    }

    /**
     * AutomatonBase
     *
     * ensure
     * specIa.inputs < implIa.inputs
     * &&
     * specIa.outputs > implIa.outputs
     * @param abstractIa IA Spec
     * @param specificIa IA Impl
     * @return true if satisfied, otherwise false
     */
    fun isInputOutputContained(specificIa: AutomatonBase, abstractIa: AutomatonBase): Boolean {
        return isInputOutputContained(abstractIa.inputActions, abstractIa.outputActions, specificIa.inputActions, specificIa.outputActions)
    }

    private fun isInputOutputContained(
        specInputs: Set<AutomatonAction>,
        specOutputs: Set<AutomatonAction>,
        implInputs: Set<AutomatonAction>,
        implOutputs: Set<AutomatonAction>
    ): Boolean {
        if (!implInputs.containsAll(specInputs)) {
            logger.debug("Implementation is missing some inputs")
            return false
        }
        if (!specOutputs.containsAll(implOutputs)) {
            logger.debug("Specification is missing some outputs")
            return false
        }
        return true
    }
}