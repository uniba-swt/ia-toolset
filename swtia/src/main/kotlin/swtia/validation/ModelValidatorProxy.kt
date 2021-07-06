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
import org.eclipse.emf.common.util.BasicDiagnostic
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.validation.Check
import org.eclipse.xtext.validation.ValidationMessageAcceptor
import swtia.ia.*
import swtia.transformation.TransformException
import swtia.transformation.iam.IamTransformController
import swtia.transformation.mia.MiaTransformationController
import swtia.util.ResourceUtil.findSharedVarIdRef
import swtia.util.ResourceUtil.findCurrentProc
import swtia.util.ResourceUtil.getAssertStmt
import swtia.util.ResourceUtil.getModel
import swtia.util.ResourceUtil.getSysStmt
import swtia.util.ResourceUtil.isGlobalVariable
import swtia.util.ResourceUtil.isIam
import swtia.util.ResourceUtil.isMia
import swtia.util.ResourceUtil.unfoldExpr
import swtia.util.Ulogger
import swtia.validation.sys.ModelInitTypeChecker
import swtia.validation.typing.DataType
import swtia.validation.typing.TypeChecker
import swtia.validation.typing.TypeException

class ModelValidatorProxy: IaValidator() {

    @Inject
    private lateinit var iamTransformController: IamTransformController

    @Inject
    private lateinit var typeChecker: TypeChecker

    @Inject
    private lateinit var initTypeChecker: ModelInitTypeChecker

    @Inject
    private lateinit var miaTransformController: MiaTransformationController

    /**
     * check the whole process, invoked by the compiler
     * steps:
     * - check cross-reference
     * - check unique identifier
     * - type checking for process template
     * - type checking for init section
     * - transform to CFG and validate
     * - transform to IAM and validate
     */
    @Check
    fun checkFullModel(model: GModel) {

        // check iam
        if (model.isIam()) {
            Ulogger.debug { "IAM model" }
        }
        if (model.isMia()) {
            Ulogger.debug { "MIA model" }
        }

        // stop if find cross reference
        if (hasCrossReferenceError(model)) {
            Ulogger.debug { "found cross reference error: " + model.eResource().errors?.first()?.toString() }
            return
        }

        // check unique name in all sets: actions, types, actions, proc, sys
        checkUniqueObjectNames(model)

        // check model header
        checkModelHeader(model)

        // 1st pass: check process for type checking
        for (proc in model.procs) {
            checkProcNoTransform(proc)
        }

        // init section
        checkInitSection(model.init)

        // stop if having error
        if (hasLocalError())
            return

        // 2nd pass: check process with transformation
        try {
            when {
                model.isIam() -> iamTransformController.transformModel(model)
                model.isMia() -> miaTransformController.transform(model)
            }
        } catch (ex: TransformException) {
            handleException(model, ex)
        }
    }

    /**
     * check data types and variables
     */
    private fun checkModelHeader(model: GModel) {
        // support MIA only
        if (model.isMia()) {

            // do not allow custom data types
            if (model.dataTypes.isNotEmpty()) {
                for (i in model.dataTypes.indices) {
                    valError(model, ErrorMessages.dataTypesNotAllowed, IaPackage.Literals.GMODEL__DATA_TYPES, i)
                }
            }

            // do not allow variables
            if (model.decls.isNotEmpty()) {
                for (i in model.decls.indices) {
                    valError(model, ErrorMessages.variableNotAllowedInMia, IaPackage.Literals.GMODEL__DECLS, i)
                }
            }
        }
    }

    /**
     * check if having invalid cross-reference
     */
    private fun hasCrossReferenceError(model: GModel): Boolean {
        return model.eResource().errors.isNotEmpty()
    }

    /**
     * check whether any error was recorded
     */
    private fun hasLocalError(): Boolean {
        val ch = this.chain
        if (ch is BasicDiagnostic) {
            return ch.children.isNotEmpty()
        }

        return false
    }

    /**
     * check single process
     * - go through all statements
     */
    private fun checkProcNoTransform(proc: GProc) {

        // check process signature
        if (proc.getModel().isMia()) {
            if (proc.params.isNotEmpty()) {
                for ((i, param) in proc.params.withIndex()) {
                    if (param.dataType != null) {
                        valError(proc, ErrorMessages.variableNotAllowedInMia, IaPackage.Literals.GPROC__PARAMS, i)
                    }
                }
            }
        }

        // check body
        checkCmpBody(proc.body)

        // find all child
        for (child in proc.eAllContents()) {
            if (child is GStmt) {
                checkStmt(child)
            } else if (child is GExpr) {
                checkExpr(child)
            }
        }
    }

    private fun handleException(model: GModel, ex: TransformException) {
        if (ex.locations.isNotEmpty()) {
            for (location in ex.locations) {
                this.acceptError(ex.message, model, location.offset, location.length, null, "")
            }
        } else {
            this.acceptError(ex.message, model, 0, 0, null, "")
        }
    }

    private fun checkCmpBody(body: GCmpStmtBody) {
        for (decl in body.decls) {
            valError(decl, ErrorMessages.localVariableIsNotSupported(decl.name), IaPackage.Literals.GCOMMON_VAR__NAME)
        }
    }

    private fun checkStmt(stmt: GStmt) {
        when (stmt) {
            is GCmpStmt -> checkCmpBody(stmt.body)
            is GIfStmt -> checkIfStmt(stmt)
            is GCaseStmt -> checkCaseStmtBody(stmt.caseBody)
            is GWhileStmt -> checkCaseStmtBody(stmt.caseBody)
            is GLoopStmt -> checkCmpBody(stmt.stmt.body)
            is GExprStmt -> checkExprStmt(stmt)
            is GAssertStmt -> checkAssertStmt(stmt)
            is GLabeledStmt -> {}
            is GJumpStmt -> checkJumpStmt(stmt)
            is GErrorSkipStmt -> {}
            is GGotoStmt -> {}
            is GDisjunctiveStmt -> {
                if (!stmt.getModel().isMia()) {
                    valError(stmt, ErrorMessages.disjunctiveMustStatementNotAllowed, IaPackage.Literals.GDISJUNCTIVE_STMT__FIRST_STMT)
                }
            }
        }
    }

    private fun checkIfStmt(stmt: GIfStmt) {
        try {
            typeChecker.ensureBoolOrAction(stmt.expr)

            // make sure no else branch if expr is action
            if (stmt.expr.unfoldExpr() is GActionExpr) {
                if (stmt.elseBody != null) {
                    valError(stmt, ErrorMessages.elseBranchNotAllowedWithAction, IaPackage.Literals.GIF_STMT__ELSE_BODY)
                }
            }
        } catch (ex: TypeException) {
            valError(stmt, ex.message!!, IaPackage.Literals.GIF_STMT__EXPR)
        }
    }

    private fun checkCaseStmtBody(caseStmtBody: GCaseStmtBody) {
        for ((index, value) in caseStmtBody.branches.withIndex()) {
            try {
                typeChecker.ensureBoolOrAction(value.expr)
            } catch (ex: TypeException) {
                valError(caseStmtBody, ex.message!!, IaPackage.Literals.GCASE_STMT_BODY__BRANCHES, index)
            }
        }
    }

    private fun checkExprStmt(stmt: GExprStmt) {
        try {
            typeChecker.ensureBoolOrAction(stmt.expr)
        } catch (ex: TypeException) {
            valError(stmt, ex.message!!, IaPackage.Literals.GEXPR_STMT__EXPR)
        }
    }

    private fun checkAssertStmt(stmt: GAssertStmt) {
        try {
            typeChecker.ensure(stmt.expr, DataType.ofBool())
        } catch (ex: TypeException) {
            valError(stmt, ex.message!!, IaPackage.Literals.GASSERT_STMT__EXPR)
        }
    }

    private fun checkJumpStmt(stmt: GJumpStmt) {
        if (stmt.jumpType == GJumpType.BREAK || stmt.jumpType == GJumpType.CONTINUE) {
            // make sure inside a loop or while
            if (!findOuterLoopOrWhile(stmt)) {
                val name = if (stmt.jumpType == GJumpType.BREAK) "break" else "continue"
                val attr = IaPackage.Literals.GJUMP_STMT__JUMP_TYPE
                valError(stmt, ErrorMessages.cannotJumpOutsideLoop(name), attr)
            }
        }
    }

    private fun checkExpr(expr: GExpr) {
        when (expr) {
            is GActionExpr ->  checkActionExpr(expr)
            is GParenthesizedExpr -> {}
            is GIdRefExpr -> checkIdRefExpr(expr)
            else -> if (expr.getModel().isMia()) {
                valError(expr.eContainer(), ErrorMessages.expressionNotAllowed, expr.eContainingFeature())
            }
        }
    }

    private fun checkActionExpr(actionExpr: GActionExpr) {

        // make sure weak action is not allowed in IAM
        if (actionExpr.isMay) {
            if (!actionExpr.getModel().isMia()) {
                valError(actionExpr, ErrorMessages.mayActionNotAllowed, IaPackage.Literals.GACTION_EXPR__MAY)
            }
        }

        // make sure action cannot be used with other expression
        if (findExprOutermostParent(actionExpr) is GExpr) {
            valError(actionExpr, ErrorMessages.actionNotAllowedInExpr, IaPackage.Literals.GACTION_EXPR__ACTION)
        }

        // check if init
        if (actionExpr.getSysStmt() == null) {

            // make sure action is declared in the process
            val proc = actionExpr.findCurrentProc()
            val declaredActions = proc.actionRefs

            // make sure usage of action is valid
            val ref = declaredActions.find { a -> a.action == actionExpr.action }
            if (ref != null) {
                if (actionExpr.suffix != ref.suffix) {
                    valError(actionExpr, ErrorMessages.invalidActionSuffix(actionExpr.action.name, ref.suffix, actionExpr.suffix), IaPackage.Literals.GACTION_EXPR__SUFFIX)
                }
            }
        }
    }

    private fun checkIdRefExpr(refExpr: GIdRefExpr) {
        if (refExpr.isVarPrime) {

            // make sure prime is for global variable only
            if (!refExpr.ref.isGlobalVariable()) {
                valError(refExpr, ErrorMessages.errorPrimeIsForGlobalVarOnly, IaPackage.Literals.GID_REF_EXPR__VAR_PRIME)
            }

            // make sure prime is used in in assume stmt
            if (refExpr.getAssertStmt() == null) {
                valError(refExpr, ErrorMessages.errorPrimeIsOnlyAllowedInAssert, IaPackage.Literals.GID_REF_EXPR__VAR_PRIME)
            }
        }
    }

    private fun checkInitSection(init: GModelInit) {
        for (item in init.items) {
            when (item) {
                is GSysDeclStmt -> checkSysDeclStmt(item)
                is GSysExprStmt -> {}
            }
        }

        // check all expr
        for (sysExpr in init.eAllContents().asSequence().filterIsInstance<GSysExpr>()) {
            checkSysExpr(sysExpr)
        }
    }

    private fun checkSysDeclStmt(stmt: GSysDeclStmt) {
        try {
            initTypeChecker.validateDecl(stmt)
        } catch (ex: TypeException) {
            valError(stmt, ex.errorMsg, IaPackage.Literals.GSYS_DECL_STMT__NAME)
        }
    }

    private fun checkSysExpr(expr: GSysExpr) {
        when (expr) {
            is GSysProcCallExpr -> checkSysProcCallExpr(expr)
            is GSysDeclRefExpr -> checkSysDeclRefExpr(expr)
            is GSysBinOpExpr -> {}
            is GSysPruneExpr -> {}
            is GSysRestrictExpr -> {}
        }
    }

    private fun checkSysProcCallExpr(expr: GSysProcCallExpr) {
        // make sure variable matched
        val args = expr.args
        val params = expr.proc.params
        if (args.size !=params.size) {
            valError(expr, ErrorMessages.errorProcCallMismatchedArgs(params.size, args.size), IaPackage.Literals.GSYS_PROC_CALL_EXPR__ARGS)
        }

        // matching type
        for ((i, arg) in args.withIndex()) {
            val param = params[i]
            try {

                // make sure no shared variable
                if (arg.findSharedVarIdRef() != null) {
                    valError(expr, ErrorMessages.sharedVariableIsNotAllowed, IaPackage.Literals.GSYS_PROC_CALL_EXPR__ARGS, i)
                    return
                }

                // ensure valid type
                typeChecker.ensure(arg, typeChecker.computeType(param))
            } catch (ex: TypeException) {
                valError(expr, ex.errorMsg, IaPackage.Literals.GSYS_PROC_CALL_EXPR__ARGS, i)
            }
        }
    }

    private fun checkSysDeclRefExpr(expr: GSysDeclRefExpr) {
        checkSysDeclStmt(expr.decl)
    }

    /**
     * make sure names are unique in each set
     */
    private fun checkUniqueObjectNames(model: GModel) {
        // actions
        checkUniqueActions(model.actions)

        // data types
        checkUniqueDataTypes(model.dataTypes)

        // decls and data type members
        checkUniqueIds(model.decls.asSequence() + model.dataTypes.asSequence().flatMap { d -> d.members })

        // process
        checkUniqueProc(model.procs)
        for (proc in model.procs) {
            checkUniqueDeclaredActionsInProc(proc.actionRefs)
        }

        // init - system
        checkUniqueSysDecls(model.init.items.filterIsInstance<GSysDeclStmt>())
    }

    private fun checkUniqueActions(actions: EList<GAction>) {
        checkUniqueSet(actions, { r -> r.name }, { n -> ErrorMessages.actionIsAlreadyDefined(n) }, { IaPackage.Literals.GCOMMON_VAR__NAME })
    }

    private fun checkUniqueDataTypes(items: EList<GCustomType>) {
        checkUniqueSet(items, { i -> i.name }, { n -> ErrorMessages.customTypeIsAlreadyDefined(n) }, { IaPackage.Literals.GCUSTOM_TYPE__NAME })
    }

    private fun checkUniqueIds(decls: Sequence<GCommonVar>) {
        checkUniqueSet(decls.asIterable(), { i -> i.name }, { n -> ErrorMessages.idIsAlreadyDefined(n) }, { IaPackage.Literals.GCOMMON_VAR__NAME })
    }

    private fun checkUniqueProc(procs: List<GProc>) {
        checkUniqueSet(procs, { p -> p.name }, { name -> ErrorMessages.procIsAlreadyDefined(name) }, { IaPackage.Literals.GPROC__NAME })
    }

    private fun checkUniqueDeclaredActionsInProc(refs: List<GActionRef>) {
        checkUniqueSet(refs, { r -> r.action.name }, { n -> ErrorMessages.actionIsAlreadyDeclaredInProc(n) }, { IaPackage.Literals.GACTION_REF__ACTION })
    }

    private fun checkUniqueSysDecls(sysDecls: List<GSysDeclStmt>) {
        checkUniqueSet(sysDecls, { r -> r.name }, { n -> ErrorMessages.sysIsAlreadyDefined(n) }, { IaPackage.Literals.GSYS_DECL_STMT__NAME })
    }

    private fun <T: EObject> checkUniqueSet(items: Iterable<T>, nameFunc: (T) -> String, errFunc: (String) -> String, featureFunc: () -> EStructuralFeature) {
        val ids = mutableSetOf<String>()
        for (item in items) {
            val name = nameFunc(item)
            if (ids.contains(name)) {
                valError(item, errFunc(name), featureFunc())
            } else {
                ids.add(name)
            }
        }
    }

    /**
     * post an validation error
     */
    private fun valError(source: EObject, message: String, feature: EStructuralFeature, index: Int = ValidationMessageAcceptor.INSIGNIFICANT_INDEX, code: String? = null) {
        error(message, source, feature, index, code)
    }

    companion object {
        private fun findOuterLoopOrWhile(obj: EObject): Boolean {
            if (obj.eContainer() == null)
                return false

            val container = obj.eContainer()
            if (container is GWhileStmt || container is GLoopStmt)
                return true

            if (container is GProc) {
                return false
            }

            if (container is GModel) {
                return false
            }

            return findOuterLoopOrWhile(container)
        }

        private fun findExprOutermostParent(expr: EObject): EObject {
            if (expr.eContainer() is GParenthesizedExpr) {
                return findExprOutermostParent(expr.eContainer())
            }

            return expr.eContainer()
        }

    }
}