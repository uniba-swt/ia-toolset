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

@file:JvmName("IdeMain")
package swtia

import kotlin.jvm.JvmStatic
import org.eclipse.xtext.ide.server.SocketServerLauncher
import swtia.ide.CustomSocketServerLauncher
import org.eclipse.xtext.ide.server.ServerLauncher
import swtia.debugger.launchDebugger
import swtia.ide.IaServerModule
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

object IdeMain {
    /**
     *
     * Run:
     * ia-ide-server -debug <ARGS> (debug)
     * ia-ide-server -lsp <ARGS> (lsp)
     *
     *
     * FOR DEBUGGER:
     * Arguments for localhost socket: -port
     * Default port is 8989
     *
     * FOR LSP:
     * Arguments for default launcher: -log -trace -debug
     * Arguments for localhost socket: -port NUMBER
     * Example: -port 8686
     *
     * @param args arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            System.err.println("Missing argument: -debug for Debugger or -lsp for LSP")
            exitProcess(1)
        }

        val postArgs = args.asList().subList(1, args.size).toTypedArray()

        when (args.first()) {
            "-lsp" -> startLsp(postArgs)
            "-debug" -> startDebugger(postArgs)
        }
    }

    private fun startDebugger(postArgs: Array<String>) {
        launchDebugger(postArgs)
    }

    private fun startLsp(postArgs: Array<String>) {
        if (postArgs.any { s -> s.startsWith(SocketServerLauncher.PORT) }) {
            CustomSocketServerLauncher().launch(postArgs)
        } else {
            ServerLauncher.launch(IdeMain::class.java.name, postArgs, IaServerModule())
        }
    }
}