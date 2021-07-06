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

@file:JvmName("DebuggerMain")
package swtia.debugger

import ch.qos.logback.classic.Level
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher
import org.eclipse.xtext.ide.server.SocketServerLauncher
import swtia.debugger.util.Util
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.Channels

fun launchDebugger(args: Array<String>) {

    // set log level
    Util.setRootLogLevel(Level.TRACE)

    // find port
    if (args.any { s -> s.startsWith(SocketServerLauncher.PORT) }) {
        SocketLauncher().launch()
    } else {
        SystemIoLauncher().launch()
    }
}

class SystemIoLauncher: DebugProtocolLauncher() {
    override fun launch() {
        this.start(System.`in`, System.out)
    }
}

class SocketLauncher: DebugProtocolLauncher() {
    override fun launch() {
        try {
            val addr = InetSocketAddress("0.0.0.0", 8989)
            val channel = AsynchronousServerSocketChannel.open()
            channel.bind(addr)
            logger.info("starting socket server at $addr")

            // do not need to support multiple threads here, sequential execution is fine
            // we only accept 1 client at one time
            while (true) {
                val socketChannel: AsynchronousSocketChannel = channel.accept().get()
                logger.info("accept new client: ${socketChannel.remoteAddress}")
                val input = Channels.newInputStream(socketChannel)
                val output = Channels.newOutputStream(socketChannel)
                this.start(input, output)
            }
        } catch (ex: IOException) {
            logger.error(ex.toString())
        }
    }

}

abstract class DebugProtocolLauncher {

    protected val logger = Util.getLogger(javaClass)

    abstract fun launch()

    protected fun start(input: InputStream, output: OutputStream) {
        val protocolServer = IaDebugProtocolServer()
        val serverLauncher: Launcher<IaDebugProtocolClient> = DebugLauncher.createLauncher(
            protocolServer,
            IaDebugProtocolClient::class.java, input, output
        )
        protocolServer.connect(serverLauncher.remoteProxy)
        logger.info("BEGIN startListening")
        serverLauncher.startListening()
        logger.info("END startListening")
    }
}
