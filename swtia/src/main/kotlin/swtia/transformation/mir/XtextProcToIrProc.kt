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

package swtia.transformation.mir

import ialib.core.AutomatonAction
import ialib.iam.expr.MActionExpr
import ialib.iam.expr.MCustomTypeLiteralExpr
import ialib.iam.expr.MExpr
import swtia.common.expr.ConversionUtil.toMDataType
import swtia.common.expr.XtextToMExprConverter
import swtia.common.expr.XtextToMExprConverter.Companion.convertToMExpr
import swtia.ia.*
import swtia.transformation.TransformException
import swtia.util.ResourceUtil.convertToMAction
import swtia.util.ResourceUtil.getLocation
import swtia.util.ResourceUtil.isActionParam
import swtia.util.ResourceUtil.toAutomatonActionType

class XtextProcToIrProc private constructor(private val proc: GProc, args: List<MExpr>) {

    private val mapArgs = run {
        val map = mutableMapOf<GProcParam, MExpr>()
        for ((i, param) in proc.params.withIndex()) {
            map[param] = args[i]
        }
        map
    }

    private val mapLabel = mutableMapOf<String, IrLabelStmt>()

    fun convert(): IrProc {

        // first round convert labels
        for (stmt in proc.body.stmts) {
            if (stmt is GLabeledStmt) {
                mapLabel[stmt.name] = IrLabelStmt(stmt.name, stmt.getLocation())
            }
        }

        // actions
        val actions = mutableSetOf<AutomatonAction>()
        for (ref in proc.actionRefs) {
            actions.add(ref.convertToMAction())
        }

        // add from params
        for (arg in this.mapArgs.values) {
            if (arg is MActionExpr) {
                actions.add(arg.action)
            }
        }

        // stmts
        val stmts = proc.body.stmts.map { stmt -> convertStmt(stmt) }
        return IrProc(proc.name, actions.toList(), stmts)
    }

    private fun convertStmt(stmt: GStmt): IrStmt {
        val loc = stmt.getLocation()
        return when (stmt) {
            is GLabeledStmt -> getIrLabel(stmt)
            is GGotoStmt -> IrGotoStmt(getIrLabel(stmt.label), loc)
            is GExprStmt -> IrExprStmt(convertExpr(stmt.expr), loc)
            is GAssertStmt -> IrAssertStmt(stmt.name, convertExpr(stmt.expr), loc)
            is GErrorSkipStmt -> IrErrorSkipStmt(stmt.isError, loc)
            is GCaseStmt -> {
                val arms = stmt.caseBody.branches.map { branch ->
                    if (branch.stmt is GGotoStmt) {
                        val goto = branch.stmt as GGotoStmt
                        val irLabel = getIrLabel(goto.label)
                        Pair(convertExpr(branch.expr), irLabel)
                    } else {
                        throw TransformException(
                            "Unexpected stmt in case body, only goto is allowed: ${branch.stmt}",
                            branch.stmt.getLocation()
                        )
                    }
                }
                IrCaseStmt(arms, loc)
            }
            is GDisjunctiveGotoStmt -> {
                IrDisjunctiveGotoStmt(stmt.labels.map { lb -> getIrLabel(lb) }, loc)
            }
            else -> error("stmt is not supported: $stmt")
        }
    }

    private fun getIrLabel(label: GLabeledStmt): IrLabelStmt {
        return mapLabel[label.name]!!
    }

    private fun convertExpr(expr: GExpr): MExpr {
        return XtextToMExprConverter(mapArgs).convert(expr)
    }

    companion object {

        /**
         * transform process using supplied arg
         */
        fun GProc.toIrProc(args: List<GExpr>): IrProc {
            return XtextProcToIrProc(this, args.map { arg -> arg.convertToMExpr() }).convert()
        }

        /**
         * No supplied argument, use default value
         */
        fun GProc.toIrProcWithDefaults(): IrProc {
            val args: List<MExpr> = this.params.map { param -> getDefaultValue(param) }
            return XtextProcToIrProc(this, args).convert()
        }

        private fun getDefaultValue(param: GProcParam): MExpr {
            val loc = param.getLocation()
            return when {
                param.isActionParam() -> MActionExpr(AutomatonAction(param.name, param.suffix.toAutomatonActionType()), false, loc)
                param.dataType.customType != null -> {
                    val typ = param.dataType.customType.toMDataType()
                    MCustomTypeLiteralExpr(typ, typ.values.first(), loc)
                }
                param.dataType.isBool -> MExpr.constBool(false, loc)
                param.dataType.isInt -> MExpr.constInt(0, loc)
                else -> error("param is not supported $param")
            }
        }
    }
}