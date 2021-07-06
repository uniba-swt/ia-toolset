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

package swtia.util

import org.apache.log4j.*

internal object Ulogger {

    private const val ConsoleRedColor = "\u001B[31m"

    private const val ConsoleResetColor = "\u001B[0m"

    private val logger = Logger.getRootLogger()

    val isDebug: Boolean
        get() = logger.level === Level.DEBUG

    val isVerbose: Boolean
        get() = logger.level == Level.INFO

    val isVerboseOrDebug: Boolean
        get() = isDebug || isVerbose

    fun debug(msgSupplier: () -> String) {
        if (!isDebug) return
        logger.debug(msgSupplier())
    }

    fun debug(fmt: String, vararg args: Any?) {
        logger.debug(String.format(fmt, *args))
    }

    fun info(fmt: String, vararg args: Any?) {
        logger.info(String.format(fmt, *args))
    }

    fun warn(fmt: String, vararg args: Any?) {
        logger.warn(String.format(fmt, *args))
    }

    fun error(fmt: String, vararg args: Any?) {
        logger.error(String.format(fmt, *args))
    }

    fun printError(prefix: String, msg: String) {
        error("$prefix : $msg")
        System.err.printf("%s%s:%s %s%s", ConsoleRedColor, prefix, ConsoleResetColor, msg, System.lineSeparator())
    }
    fun printError(s: String) {
        printError("error", s)
    }

    fun setup(debug: Boolean, verbose: Boolean) {
        // set log level and pattern
        val rootLogger = Logger.getRootLogger()
        rootLogger.level = when {
            debug -> Level.DEBUG
            verbose -> Level.INFO
            else -> Level.WARN
        }
        val layout = PatternLayout("%d{ISO8601} %-5p: %m%n")
        rootLogger.removeAllAppenders()
        rootLogger.addAppender(ConsoleAppender(layout))
    }

    fun print(msg: String) {
        debug { msg }
        println(msg)
    }

    fun exception(ex: Exception) {
        if (isDebug) {
            debug { ex.stackTraceToString() }
        }
    }
}