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

package swtia.ide.codelens

import ialib.iam.expr.MLocation
import org.eclipse.lsp4j.*
import org.eclipse.xtext.ide.server.Document
import org.eclipse.xtext.ide.server.codelens.ICodeLensResolver
import org.eclipse.xtext.ide.server.codelens.ICodeLensService
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.util.CancelIndicator
import swtia.ia.GSysBinOpExpr
import swtia.ia.GSysBinOpType
import swtia.util.ResourceUtil.getLocation
import swtia.util.ResourceUtil.getRootModel
import java.nio.file.Paths


class CodeLensService: ICodeLensService, ICodeLensResolver {
    override fun computeCodeLenses(
        document: Document,
        resource: XtextResource,
        params: CodeLensParams,
        indicator: CancelIndicator
    ): List<CodeLens> {

        val uri = resource.uri.path()

        // run vs debug
        val model = resource.getRootModel() ?: return emptyList()
        val initSection = model.init
        val loc = initSection.getLocation()

        // init run and debug
        val lenRun = CodeLens().also { len ->
            len.command = Command().also { cmd ->
                cmd.command = "ia-toolset.cmdRun"
                cmd.title = "Run"
                cmd.arguments = listOf(uri)
            }
            len.range = getRange(loc)
        }

        val lenDebug = CodeLens().also { len ->
            len.command = Command().also { cmd ->
                cmd.command = "ia-toolset.cmdDebug"
                cmd.title = "Debug"
                cmd.arguments = listOf(uri)
            }
            len.range = getRange(loc)
        }

        val list = mutableListOf(lenRun, lenDebug)

        // codelens for exploring the process
        for (proc in model.procs) {
            list.add(CodeLens().also { len ->
                len.command = Command().also { cmd ->
                    cmd.command = "ia-toolset.cmdExploreProc"
                    cmd.title = "Explore"
                    cmd.arguments = listOf(uri, proc.name)
                }
                len.range = getRange(proc.getLocation())
            })
        }

        // codelens for refinement simulator
        for (item in initSection.items) {
            if (item.expr is GSysBinOpExpr) {
                val expr = item.expr as GSysBinOpExpr
                if (expr.opType == GSysBinOpType.REFINE) {

                    // simulate
                    list.add(CodeLens().also { len ->
                        len.command = Command().also { cmd ->
                            cmd.command = "ia-toolset.cmdSimulateRefinement"
                            cmd.title = "Simulate"
                            cmd.arguments = listOf(uri)
                        }
                        len.range = getRange(expr.getLocation())
                    })


                    // counter example
                    list.add(CodeLens().also { len ->
                        len.command = Command().also { cmd ->
                            cmd.command = "ia-toolset.simulateErrorRefinement"
                            cmd.title = "Find counter-example"
                            cmd.arguments = listOf(uri)
                        }
                        len.range = getRange(expr.getLocation())
                    })
                }
            }
        }

        return list
    }

    private fun getRange(loc: MLocation): Range {
        return Range(Position(loc.lineBegin - 1, loc.colBegin - 1), Position(loc.lineEnd - 1, loc.colEnd - 1))
    }

    override fun resolveCodeLens(
        document: Document,
        resource: XtextResource,
        codeLens: CodeLens,
        indicator: CancelIndicator
    ): CodeLens {
        codeLens.command.title = "${codeLens.command.title} (RESOLVED)"
        return codeLens
    }
}