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

package ialib.iam.simulation

import com.google.common.collect.Sets
import ialib.core.simulation.RefinementType
import ialib.iam.MemState
import ialib.iam.MemStep
import ialib.iam.expr.MExpr
import ialib.iam.expr.MLogicBinExpr
import ialib.iam.expr.MLogicOp
import ialib.iam.expr.solver.DefaultSmtSolver
import org.apache.log4j.Logger

class RefinementFamilyProvider(
    private val solver: DefaultSmtSolver,
    private val familiesSpecStep: MemStep,
    private val familiesType: RefinementType,
    private val checkingInput: Boolean,
    private val specStep: MemStep,
    private val implSteps: List<MemStep>
) {

    private val logger = Logger.getLogger(RefinementFamilyProvider::javaClass.javaClass)
    private val validSet = mutableSetOf<Set<Int>>()

    /**
     * go through the combination
     *
     * 1. set the index for impl steps
     * 2. go through the step: 0, 1, 2, ... N
     * 3. incremental go the the next level: 0 1, 0 2, .., 0 N
     * 4. next level: 0 1 2,..., ... N
     * 5. final level: 0 1 2 3 .. N
     */
    fun exec(): List<RefinementFamily> {
        val maxLevel = implSteps.size

        // start
        for (level in 1..maxLevel) {
            processLevel(level, specStep, implSteps)
        }

        // update families
        val refinedType = if (checkingInput) RefinementType.Impl else RefinementType.Spec
        return validSet.map {
            RefinementFamily(refinedType, it.map { index -> createRefinedStep(implSteps[index]) })
        }
    }

    private fun createRefinedStep(memStep: MemStep): RefinementStep {
        return RefinementStep(memStep, createDstState(memStep.dstState))
    }

    private fun createDstState(dstState: MemState): SimMemState {
        if (familiesType == RefinementType.Spec) {
            return SimMemState(familiesSpecStep.dstState, dstState)
        }

        return SimMemState(dstState, familiesSpecStep.dstState)
    }

    private fun processLevel(
        level: Int,
        specStep: MemStep,
        steps: List<MemStep>,
    ) {
        logger.debug("--------------------------------------------------------")
        logger.debug("--- process new level: $level with ignored: $validSet")
        logger.debug("--------------------------------------------------------")

        val combinations: Set<Set<Int>> = Sets.combinations(steps.indices.toSet(), level)
        for (comSet in combinations) {

            logger.debug("\ntry check family: $comSet")
            if (validSet.any{ comSet.containsAll(it) }) {
                logger.debug("\tIGNORED")
                continue
            }

            // compute
            val items: List<MemStep> = comSet.map { c -> steps[c] }
            if (validateFamily(specStep, items)) {
                logger.debug("\tadd: $comSet to ignore set")
                validSet.add(comSet)
            }
        }
    }

    private fun validateFamily(specStep: MemStep, items: List<MemStep>): Boolean {
        logger.debug("\n\tcheck family: $items")
        // first check pre condition
        val or = makeOrOrPreConditions(items)
        if (!satImplies(specStep.preCond, or)) {
            logger.debug("\tinvalid pre-condition")
            return false
        }

        // make sure all post-conditions of all members are valid, no matter it is input or output
        for (item in items) {
            val and = makeAnd(makeAnd(specStep.preCond, item.preCond), item.postCond)
            if (!satImplies(and, specStep.postCond))
                return false
        }

        return true
    }

    private fun makeOrOrPreConditions(items: List<MemStep>): MExpr {
        var src = items[0].preCond
        for (i in 1 until items.size) {
            src = makeOr(src, items[i].preCond)
        }

        return src
    }

    private fun makeAnd(first: MExpr, second: MExpr): MExpr {
        return MLogicBinExpr(first, second, MLogicOp.AND)
    }

    private fun makeOr(first: MExpr, second: MExpr): MExpr {
        return MLogicBinExpr(first, second, MLogicOp.OR)
    }

    private fun satImplies(condition: MExpr, check: MExpr): Boolean {
        return solver.solveForAllImplies(condition, check)
    }
}
