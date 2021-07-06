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

package ialib.iam.debug

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.log4j.Logger

class DebugSession private constructor(){

    private val logger = Logger.getLogger("DebugSession")

    private val channel: Channel<Boolean> = Channel(1)

    private var isPaused = false

    private val mutex = Mutex()

    fun pause() {
        logger.debug("mark as paused")
        runBlocking {
            mutex.withLock {
                isPaused = true
            }
        }
    }

    fun resume() {
        logger.debug("mark as resumed")
        runBlocking {
            mutex.withLock {
                if (isPaused) {
                    isPaused = false
                    channel.send(true)
                }
            }
        }
    }

    fun waitForResumeIfNeeded() {
        runBlocking {
            mutex.withLock { isPaused }.let { p ->
                logger.debug("start waitForResumeIfNeeded")
                if (p)
                    channel.receive()
            }
        }
        logger.debug("end waitForResumeIfNeeded")
    }

    fun shutdown() {
        resume()
    }

    companion object {

        var current: DebugSession = DebugSession()
            private set

        fun newSession() {
            current = DebugSession()
        }

        fun waitForResumeIfNeeded() {
            current.waitForResumeIfNeeded()
        }
    }
}