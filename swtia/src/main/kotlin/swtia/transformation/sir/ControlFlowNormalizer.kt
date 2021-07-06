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

import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import swtia.ia.*
import swtia.transformation.ModelFactory
import swtia.util.ResourceUtil.unfoldExpr

/**
 * Transform control-flow structs into simple IR (still valid by the language) with:
 * - Label
 * - Goto
 * - Case
 * - Error
 * - Skip
 * - Assume/Guarantee
 *
 * For the case statement, each branch is a single goto statement
 * No while, loop, if-else after this step
 *
 * Most important transformation step
 */
class ControlFlowNormalizer: ProcIrNormalizer() {

    override fun normalize(proc: GProc) {
        // go through all proc
        normalizeCmpBody(proc.body, null, null)
    }

    private var labelCounter = 0

    private val factory = ModelFactory.factory

    private fun normalizeStmt(stmt: GStmt, loopBeginLbStmt: GLabeledStmt? = null, loopEndLbStmt: GLabeledStmt? = null) {

        when (stmt) {
            is GIfStmt -> normalizeIfStmt(stmt, loopBeginLbStmt, loopEndLbStmt)
            is GLoopStmt -> normalizeLoopStmt(stmt)
            is GWhileStmt -> normalizeWhileStmt(stmt)
            is GCmpStmt -> normalizeCmpBody(stmt.body, loopBeginLbStmt, loopEndLbStmt)
            is GCaseStmt -> normalizeCaseStmt(stmt, loopBeginLbStmt, loopEndLbStmt)
            is GDisjunctiveStmt -> normalizeDisjunctiveStmt(stmt, loopBeginLbStmt, loopBeginLbStmt)
            is GJumpStmt -> {
                if (loopBeginLbStmt != null && loopEndLbStmt != null) {
                    normalizeJump(stmt, loopBeginLbStmt, loopEndLbStmt)
                } else {
                    error("missing label for loop begin or end")
                }
            }
        }
    }

    private fun normalizeCmpBody(body: GCmpStmtBody, loopBeginLbStmt: GLabeledStmt? = null, loopEndLbStmt: GLabeledStmt? = null) {
        body.stmts.forEach { stmt -> normalizeStmt(stmt, loopBeginLbStmt, loopEndLbStmt) }
    }

    private fun normalizeIfStmt(stmt: GIfStmt, loopBeginLbStmt: GLabeledStmt?, loopEndLbStmt: GLabeledStmt?) {
        val expr = stmt.expr.unfoldExpr()

        // normalize child first
        normalizeStmt(stmt.thenBody, loopBeginLbStmt, loopEndLbStmt)
        stmt.elseBody?.let { normalizeStmt(it, loopBeginLbStmt, loopEndLbStmt) }

        // change to switch case
        val caseStmt = factory.createGCaseStmt()
        caseStmt.caseBody = factory.createGCaseStmtBody()

        // add 2 branches, create empty else statement if missing
        caseStmt.caseBody.branches.add(ModelFactory.createBranch(copy(expr), stmt.thenBody))
        if (expr !is GActionExpr && expr is GOpExpr) {
            val elseBody = stmt.elseBody ?: factory.createGCmpStmt().also { it.body = factory.createGCmpStmtBody() }
            caseStmt.caseBody.branches.add(ModelFactory.createBranch(ModelFactory.not(copy(expr)), elseBody))
        }

        // replace
        replace(stmt, caseStmt)

        // normalize new case stmt
        normalizeCaseStmt(caseStmt, loopBeginLbStmt, loopEndLbStmt)
    }

    private fun normalizeLoopStmt(stmt: GLoopStmt) {
        val lb = newLabel()
        val loopBeginLbStmt = factory.createGLabeledStmt().also { it.name = "${lb}_loop_begin" }
        val loopEndLbStmt = factory.createGLabeledStmt().also { it.name = "${lb}_loop_end" }

        // go through all child
        stmt.stmt.body.stmts.forEach { child -> normalizeStmt(child, loopBeginLbStmt, loopEndLbStmt) }

        // create new cmp stmt
        val cmpBody = factory.createGCmpStmtBody().also {
            it.stmts.add(loopBeginLbStmt)
            it.decls.addAll(stmt.stmt.body.decls)
            it.stmts.addAll(stmt.stmt.body.stmts)
            it.stmts.add(factory.createGGotoStmt().also { it.label = loopBeginLbStmt })
            it.stmts.add(loopEndLbStmt)
        }

        val newStmt = factory.createGCmpStmt().also {
            it.body = cmpBody
        }

        replace(stmt, newStmt)
    }

    private fun normalizeWhileStmt(stmt: GWhileStmt) {
        // create new case stmt
        val caseStmt = factory.createGCaseStmt().also { it.caseBody = stmt.caseBody }

        val cmpStmt = factory.createGCmpStmt().also { cmp ->
            cmp.body = factory.createGCmpStmtBody().also { body ->
                body.stmts.add(caseStmt)
            }
         }

        // transform to loop and case
        val loopStmt = factory.createGLoopStmt().also { it.stmt = cmpStmt }
        replace(stmt, loopStmt)

        // normalize new loop
        normalizeLoopStmt(loopStmt)
    }

    private fun normalizeCaseStmt(stmt: GCaseStmt, loopBeginLbStmt: GLabeledStmt?, loopEndLbStmt: GLabeledStmt?) {

        // check if each branch has only 1 statement goto
        if (stmt.caseBody.branches.all { b -> b.stmt is GGotoStmt }) {
            return
        }

        // go through each case body
        for (branch in stmt.caseBody.branches) {
            normalizeStmt(branch.stmt, loopBeginLbStmt, loopEndLbStmt)
        }

        // convert the case statement into a compound statement which contains labels for each branch
        // new case stmt contains only guards and goto statement
        val newCmpBody = factory.createGCmpStmtBody()
        val newCaseStmt = factory.createGCaseStmt().also { it.caseBody = factory.createGCaseStmtBody() }
        newCmpBody.stmts.add(newCaseStmt)

        // transform each case to label and jump
        val baseLb = newLabel()
        val lbCaseEnd = factory.createGLabeledStmt().also { it.name = "${baseLb}_case_end" }
        for ((i, branch) in stmt.caseBody.branches.withIndex()) {
            val lb = "${baseLb}_case_$i"

            // create new label and add body
            val lbStmt = factory.createGLabeledStmt().also { it.name = lb }
            newCmpBody.stmts.add(lbStmt)
            newCmpBody.stmts.add(branch.stmt)
            newCmpBody.stmts.add(factory.createGGotoStmt().also { it.label = lbCaseEnd })

            // replace branch body by a single go to stmt
            val gotoStmt = factory.createGGotoStmt().also { it.label = lbStmt }

            // create new case
            val newBranch = factory.createGCaseBranch().also {
                it.expr = branch.expr
                it.stmt = gotoStmt
            }
            newCaseStmt.caseBody.branches.add(newBranch)
        }

        newCmpBody.stmts.add(lbCaseEnd)

        // replace new cmp
        val newCmpStmt = factory.createGCmpStmt().also { it.body = newCmpBody }
        replace(stmt, newCmpStmt)
    }

    private fun normalizeDisjunctiveStmt(stmt: GDisjunctiveStmt, loopBeginLbStmt: GLabeledStmt?, loopEndLbStmt: GLabeledStmt?) {
        // convert the disjunctive must-statement into a compound statement which contains labels for each child recursively
        // example: i? or o! or tau
        // result: or { lb_0, lb_1, lb_2 }
        val newDisjunctiveStmt = factory.createGDisjunctiveGotoStmt()
        val newCmpBody = factory.createGCmpStmtBody().also {
            it.stmts.add(newDisjunctiveStmt)
        }

        // create label end
        val baseLb = newLabel() + "_or"
        val lbEnd = factory.createGLabeledStmt().also { it.name = "${baseLb}_end" }

        // convert each child to label and use goto
        val seq = getDisjunctiveChildren(stmt)
        for ((i, child) in seq.withIndex()) {
            val lb = "${baseLb}_$i"
            val lbStmt = factory.createGLabeledStmt().also { it.name = lb }
            newCmpBody.stmts.add(lbStmt)
            newCmpBody.stmts.add(child)
            newCmpBody.stmts.add(factory.createGGotoStmt().also { it.label = lbEnd })

            // set
            newDisjunctiveStmt.labels.add(lbStmt)

            // transform child
            normalizeStmt(child, loopBeginLbStmt, loopEndLbStmt)
        }

        // add label end
        newCmpBody.stmts.add(lbEnd)

        // replace new cmp
        val newCmpStmt = factory.createGCmpStmt().also { it.body = newCmpBody }
        replace(stmt, newCmpStmt)
    }

    /**
     * Get Disjunctive children recursively
     */
    private fun getDisjunctiveChildren(stmt: GDisjunctiveStmt): Sequence<GStmt> {
        return sequence {
            for (child in sequenceOf(stmt.firstStmt, stmt.secondStmt)) {
                if (child is GDisjunctiveStmt) {
                    yieldAll(getDisjunctiveChildren(child))
                } else {
                    yield(child)
                }
            }
        }
    }

    private fun normalizeJump(stmt: GJumpStmt, loopLabelStmt: GLabeledStmt, outLabelStmt: GLabeledStmt) {
        val newGoto = when(stmt.jumpType) {
            GJumpType.CONTINUE -> ModelFactory.createGotoStmt(loopLabelStmt)
            GJumpType.BREAK -> ModelFactory.createGotoStmt(outLabelStmt)
            else -> stmt
        }

        // replace
        replace(stmt, newGoto)
    }

    private fun newLabel(suffix: String? = null) : String {
        val tmpSuffix = if (suffix != null) { "_$suffix" } else { "" }
        return "_lb_${labelCounter++}$tmpSuffix"
    }

    private fun replace(old: EObject, new: EObject) {
        EcoreUtil2.replace(old, new)
    }

    private fun <T: EObject> copy(obj: T) : T {
        return EcoreUtil2.copy(obj)
    }
}