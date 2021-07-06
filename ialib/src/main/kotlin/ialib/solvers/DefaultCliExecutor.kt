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

package ialib.solvers

import ialib.Ulogger.debugIfEnabled
import org.apache.log4j.Logger
import java.io.*

class DefaultCliExecutor {
    fun exec(command: String, inputText: String, workingDir: String?): Pair<Boolean, String?> {
        return execWithInput(command, inputText, workingDir)
    }

    fun exec(command: String, workingDir: String?): Pair<Boolean, String?> {
        return execWithInput(command, null, workingDir)
    }

    fun execRaw(command: String, inputProvider: (OutputStream) -> Unit, workingDir: String? = null): Pair<Boolean, InputStream?> {
        try {
            // find suitable working dir
            val dir = File(workingDir ?: File("").absolutePath)
            val cmd = prefixIfNeeded + command
            logger.debug(String.format("Exec: %s\nin dir: %s", cmd, dir.absolutePath))

            // execute
            val process = Runtime.getRuntime().exec(cmd, null, dir)
            process.outputStream.use(inputProvider)

            // monitor error
            var serr: String?
            BufferedReader(InputStreamReader(process.errorStream)).use { stdError ->
                while (stdError.readLine().also { serr = it } != null) {
                    logger.debug(serr)
                }
            }
            val statusCode = process.waitFor()
            logger.debugIfEnabled { "exit: $statusCode" }
            return Pair(statusCode == 0, process.inputStream)
        } catch (e: InterruptedException) {
            logger.warn(e.message, e)
        } catch (e: IOException) {
            logger.warn(e.message, e)
        }

        return Pair(false, null)
    }

    private fun execWithInput(command: String, inputText: String?, workingDir: String?): Pair<Boolean, String?> {
        val pair = execRaw(command, { writer ->
            helperWriteText(writer, inputText)
        }, workingDir)
        val sout = pair.second?.use { std -> helperReadText(std) }
        logger.debugIfEnabled { "stdout: $sout" }
        return Pair(pair.first, sout)
    }

    companion object {
        private val logger = Logger.getLogger(DefaultCliExecutor::class.java)
        private val prefixIfNeeded: String
            get() = if (ToolPathUtil.isWindows()) "cmd /c " else ""

        fun helperWriteText(outputStream: OutputStream, text: String?) {
            if (text != null) {
                outputStream.writer().use { wt -> wt.write(text) }
            }
        }

        fun helperReadText(inputStream: InputStream): String {
            return InputStreamReader(inputStream).use { rd -> rd.readText().trim() }
        }
    }
}