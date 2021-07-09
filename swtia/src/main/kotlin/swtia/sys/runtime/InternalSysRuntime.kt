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

package swtia.sys.runtime

import com.google.inject.Inject
import ialib.core.CoreException
import ialib.iam.MemAutomaton
import ialib.debug.DebugSession
import ialib.mia.ModalAutomaton
import swtia.ia.*
import swtia.sys.IaRuntimeException
import swtia.sys.IasRuntimeData
import swtia.sys.IasStmtResult
import swtia.sys.RuntimeProviderInterface
import swtia.sys.iam.IamRuntimeProvider
import swtia.sys.mia.MiaRuntimeProvider
import swtia.sys.models.SimpleProcProvider
import swtia.sys.models.SysIaBase
import swtia.util.ResourceUtil.getModel
import swtia.util.ResourceUtil.isIam
import swtia.util.ResourceUtil.isMia
import swtia.util.ResourceUtil.isTemporary
import swtia.util.Ulogger
import swtia.util.XtextModelHelper
import swtia.validation.ErrorMessages

abstract class InternalSysRuntime {
    @Inject
    private lateinit var simpleProcProvider: SimpleProcProvider

    @Inject
    private lateinit var iamRuntimeProvider: IamRuntimeProvider

    @Inject
    private lateinit var miaRuntimeProvider: MiaRuntimeProvider

    @Inject
    private lateinit var modelHelper: XtextModelHelper

    /**
     * map of IA at runtime
     */
    val data = IasRuntimeData()

    /**
     * start executing the init section (runtime)
     */
    fun execute(init: GModelInit) {
        // reset
        data.map.clear()

        // update
        onInitWillBegin(init)
        DebugSession.waitForResumeIfNeeded()

        // main execution
        executeSequence(init)

        // end int
        onInitEnded(init)
        DebugSession.waitForResumeIfNeeded()
    }

    /**
     * execute stmt with life cycle
     */
    private fun executeSequence(modelInit: GModelInit) {
        for (stmt in modelInit.items) {

            // begin stmt
            if (!stmt.isTemporary()) {
                onStmtWillBegin(stmt)
                DebugSession.waitForResumeIfNeeded()
            }

            // execute
            try {
                when {
                    modelInit.getModel().isMia() -> executeStmt(miaRuntimeProvider, stmt)
                    modelInit.getModel().isIam() -> executeStmt(iamRuntimeProvider, stmt)
                    else -> throw IaRuntimeException("Stmt is not supported")
                }
            } catch (ex: IaRuntimeException) {
                onExceptionCaught(ex)
                DebugSession.waitForResumeIfNeeded()
            }

            // end stmt
            if (!stmt.isTemporary()) {
                onStmtEnded(stmt)
            }
        }
    }

    abstract fun onExceptionCaught(ex: IaRuntimeException)

    private fun <S: SysIaBase> executeStmt(runtimeProvider: RuntimeProviderInterface<S>, item: GSysStmt): IasStmtResult<S> {
        // process
        Ulogger.debug { "execute '${item.javaClass.name}'" }
        return when (item) {
            is GSysDeclStmt -> {
                executeExpr(runtimeProvider, item.expr, item.name).also { res ->
                    if (res.sys != null) {
                        addSys(res.sys)
                    }
                }
            }
            is GSysExprStmt -> {
                executeExpr(runtimeProvider, item.expr, tempName())
            }
            else -> throw IaRuntimeException("Stmt is not supported")
        }
    }

    /**
     * process GSysProcCallExpr | GSysDeclRefExpr | GSysBinOpExpr
     */
    private fun <S: SysIaBase> executeExpr(runtimeProvider: RuntimeProviderInterface<S>, expr: GSysExpr, name: String): IasStmtResult<S> {
        when (expr) {
            is GSysProcCallExpr -> {
                // create new proc with args is replaced
                val simpleProc = simpleProcProvider.execute(name, expr.proc, expr.args)
                return IasStmtResult.ofSys(runtimeProvider.createSysFromProc(name, simpleProc))
            }
            is GSysDeclRefExpr -> return IasStmtResult.ofSys(runtimeProvider.copy(name, data.getSys(expr.decl.name)))
            is GSysPruneExpr -> {
                return IasStmtResult.ofSys(runtimeProvider.prune(name, getSysFromParam(expr.param))).also { result ->
                    if (isInitialStateErrorAfterPruned(result.sys!!)) {
                        addSys(result.sys)
                        throw IaRuntimeException(ErrorMessages.initStateIsPruned)
                    }
                }
            }
            is GSysRestrictExpr -> return IasStmtResult.ofSys(runtimeProvider.restrict(name, getSysFromParam(expr.param), expr.actions))
            is GSysScopeExpr -> return IasStmtResult.ofSys(runtimeProvider.scope(name, getSysFromParam(expr.param), expr.actions))
            is GSysBinOpExpr -> {
                val sys1 = getSysFromParam<S>(expr.param1)
                val sys2 = getSysFromParam<S>(expr.param2)
                try {
                    return when (expr.opType) {
                        GSysBinOpType.PRODUCT -> {
                            IasStmtResult.ofSys(runtimeProvider.product(name, sys1, sys2)).also { result ->
                                if (isInitialStateErrorAfterPruned(runtimeProvider.prune(name, result.sys!!))) {
                                    addSys(result.sys)
                                    throw IaRuntimeException(ErrorMessages.incompatibleIas(sys1.name, sys2.name))
                                }
                            }
                        }
                        GSysBinOpType.REFINE -> {
                            val (result, graph) = runtimeProvider.refinement(sys1, sys2)
                            if (graph != null) {
                                data.graphs.add(graph)
                            }
                            if (!result) {
                                throw IaRuntimeException("${sys1.name} doesn't IAM-refine ${sys2.name}")
                            }

                            IasStmtResult.ofGraph(graph, result)
                        }
                        else -> throw IaRuntimeException("unexpected op type: ${expr.opType}")
                    }
                } catch (ex: CoreException) {
                    throw IaRuntimeException(ex.msg)
                }
            }
            else -> throw IaRuntimeException("expr is not supported in Ias runtime: $expr")
        }
    }

    private fun <S: SysIaBase> getSysFromParam(expr: GSysExpr): S {
        if (expr is GSysDeclRefExpr) {
            return data.getSys(expr.decl.name)
        }

        throw IaRuntimeException("invalid params for bin op, expected 'GSysDeclRefExpr' but '$expr'")
    }

    private fun <S: SysIaBase> addSys(sys: S) {
        Ulogger.debug { "runtime table add: ${sys.name}" }
        modelHelper.renderSys(sys)
        data.putSys(sys)
        Ulogger.debug { "runtime table: ${data.map.keys}" }
    }

    private fun isInitialStateErrorAfterPruned(tmpSys: SysIaBase): Boolean {
        return when (val tmpIa = tmpSys.getIa()) {
            is MemAutomaton -> tmpIa.initState.isError
            is ModalAutomaton -> tmpIa.initState.isError
            else -> false
        }
    }

    private var counter = 0
    private fun tempName(): String {
        return "__runtime_${counter++}"
    }

    abstract fun onStmtWillBegin(stmt: GSysStmt)

    abstract fun onStmtEnded(stmt: GSysStmt)

    abstract fun onInitWillBegin(init: GModelInit)

    abstract fun onInitEnded(init: GModelInit)
}