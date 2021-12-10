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

package swtia.transformation.iam

import com.google.common.collect.Sets
import com.google.inject.Inject
import ialib.core.AutomatonAction
import ialib.iam.MemStep
import ialib.iam.expr.MDeclRefExpr
import ialib.iam.expr.MExpr
import ialib.iam.expr.MLocation
import ialib.iam.expr.solver.DefaultSmtSolver
import ialib.iam.expr.translator.MExprHelper
import swtia.sys.iam.SysIa
import swtia.transformation.TransformException
import swtia.validation.ErrorMessages

class IamValidator {

    @Inject
    private lateinit var solver: DefaultSmtSolver

    /**
     * value IA
     * - pre-cond must be satisfiable
     * - post-cond must be satisfiable
     * - primed variable in post-condition
     * - data-determinism for input action
     */
    @Throws(TransformException::class)
    fun validate(ia: SysIa) {

        solver.checkMissingTools().let { tools ->
            if (tools.isNotEmpty()) {
                throw TransformException(ErrorMessages.missingTools(tools), MLocation.empty())
            }
        }

        for (state in ia.automaton.getIterator()) {
            for ((action, steps) in state.mapSteps) {
                for (step in steps) {
                    validateStep(step)
                }

                ensureInputDataDeterministic(action, steps)
            }
        }
    }

    /**
     * validate whether IAM is data-deterministic
     * for input action only
     */
    @Throws(TransformException::class)
    private fun ensureInputDataDeterministic(action: AutomatonAction, steps: List<MemStep>) {
        if (!action.isInput() || steps.size < 2) {
            return
        }

        val setSteps = steps.toSet()

        // incremental
        // go through single step to find out if it is always all
        // only need size of 2
        @Suppress("UnstableApiUsage")
        val combinations = Sets.combinations(setSteps, 2)
        for (combination in combinations) {
            if (isCombinationSat(combination)) {
                val strSteps = combination.joinToString(", ") { e -> e.toString() }
                val locations = combination.map { st -> st.action.location }
                 throw TransformException(ErrorMessages.errorDataDeterministic(action.formatted(), strSteps), locations)
            }
        }
    }

    /**
     * check if set of exprs is valid
     */
    private fun isCombinationSat(steps: Set<MemStep>): Boolean {
        // quick check if const value
        if (solver.solveAnd(steps.map { e -> e.preCond }.toList())) {
            return true
        }

        return false
    }

    /**
     * check if pre and post condition are valid
     */
    @Throws(TransformException::class)
    private fun validateStep(step: MemStep) {
        if (!solver.solve(step.preCond)) {
            throw TransformException(ErrorMessages.errorIamPreCondNotSat(step.toString()), step.preCond.getLocations().toList())
        }

        if (!solver.solve(step.postCond)) {
            throw TransformException(ErrorMessages.errorIamPostCondNotSat(step.toString()), step.postCond.getLocations().toList())
        }

        // check special restriction on post-condition
        // x > 0, i?, x' > x && x' < 0 must be invalid
        // check: forall x > 0. exist x' > x && x' < 0
        if (isPostHasReferenceToPre(step.preCond, step.postCond)) {
            if (!solver.solveAnd(step.preCond, step.postCond)) {
                throw TransformException(ErrorMessages.errorUnsatisfiablePostCond(step.toString()), step.postCond.getLocations().toList())
            }
        }
    }

    /**
     * check if: x' > x in post-cond
     */
    private fun isPostHasReferenceToPre(preCond: MExpr, postCond: MExpr): Boolean {
        val setPre = MExprHelper
            .findAllSeq(preCond) { e -> e is MDeclRefExpr }
            .map { e -> (e as MDeclRefExpr).decl.name }
            .toSet()
        val red = MExprHelper.findFirst(postCond) { e -> e is MDeclRefExpr && e.isPrime && setPre.contains(e.decl.name) }
        return red != null
    }
}
