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

package ialib.simpleia.refinement.interactive

import org.apache.log4j.Logger
import ialib.simpleia.Automaton
import ialib.simpleia.simulation.NfaStepSimulator
import ialib.simpleia.simulation.SimState
import ialib.core.simulation.SimAction
import ialib.core.simulation.RefinementType
import ialib.util.ColorUtil
import java.io.InputStream
import java.io.PrintStream
import java.util.*

/**
 * Interactive simulation controller
 */
class IntSimController(private val str: PrintStream, strIn: InputStream, private val ia1: Automaton, private val ia2: Automaton) {
    private val commands: MutableList<AbstractSimCommand>
    private val stepSimulator: NfaStepSimulator
    private val stackStates: Stack<Pair<SimState, SimAction>>
    private val backSimCommand: BackSimCommand
    private var scn: Scanner = Scanner(strIn)

    fun start() {
        str.printf("Start alternating simulation for %s (Specification) and %s (Implementation)%n", ia1.name, ia2.name)

        // main loop
        val state = SimState(ia1.initState, ia2.initState)
        mainLoop(state)
    }

    private fun mainLoop(state: SimState) {
        logger.debug("stack in main loop: " + stackStates.size)

        // find options
        commands.clear()
        updateStepsAndCommands(state)

        // print commands
        printState(state)
        printCommands()

        // wait for result
        val input = waitForSelection(commands.size - 1)
        if (input == -1) {
            str.printf("quit the interactive simulation%n")
            return
        }
        val selectedCmd = commands[input]
        if (selectedCmd is BackSimCommand) {
            mainLoop(stackStates.pop().first)
        } else if (selectedCmd is ActionSimCommand) {
            val cmd = selectedCmd
            stackStates.push(Pair(state, cmd.action))
            mainLoop(cmd.dstState)
        }
    }

    /**
     * parse selected integer from input stream
     * @param max max
     * @return -1 if quite, other number in range
     */
    private fun waitForSelection(max: Int): Int {
        while (true) {
            val input = scn.next()
            if (input.trim { it <= ' ' } == "q") {
                return -1
            }
            var num = -2
            try {
                num = input.toInt()
            } catch (ignored: NumberFormatException) {
            }

            // check
            if (num >= 0 && num <= max) {
                return num
            }
            str.printf("error: invalid input, expect number between 0 and %d or 'q' (quit)%n", commands.size - 1)
        }
    }

    private fun printState(state: SimState) {
        str.printf("%n")
        str.printf("====== Current status: %s%n", state.formattedName())
        if (state.isHasErrorAction) {
            printErrorActions(state)
        }
    }

    private fun printErrorActions(state: SimState) {
        for (action in state.errorActions) {
            var reqSt: String
            var reqAct: String
            var defSt: String
            if (action.edgeType === RefinementType.Spec) {
                reqSt = state.formattedSpec()
                reqAct = action.actionSpec.formattedString()
                defSt = state.formattedImpl()
            } else {
                reqSt = state.formattedImpl()
                reqAct = action.actionImpl.formattedString()
                defSt = state.formattedSpec()
            }
            str.print(ColorUtil.ConsoleRedColor)
            str.printf("%n* Error found: can not simulate action '%s'%n", reqAct)
            str.print(ColorUtil.ConsoleResetColor)
            str.printf("%s%s requests action '%s'%n", INDENT, reqSt, reqAct)
            str.printf("%s%s does not have a simulated action%n", INDENT, defSt)
        }
    }

    private fun printCommands() {
        str.printf("%n* Available options:%n%n")
        for (i in commands.indices) {
            str.printf("[%d] %s%n%n", i, commands[i].getCommandDesc(INDENT))
        }
        val max = commands.size - 1
        str.printf("Choose an option (%s or 'q'): ", if (max == 0) "0" else "0..$max")
    }

    private fun updateStepsAndCommands(state: SimState) {
        if (state.actions.isEmpty()) {
            stepSimulator.updateStepsToState(state)
        }

        // check to add back
        if (!stackStates.isEmpty()) {
            backSimCommand.prevState = stackStates.peek().first
            commands.add(backSimCommand)
        }

        // convert steps to action
        for (action in state.actions) {
            if (action.isError) continue
            for (dstState in state.getDstStates(action)) {
                commands.add(ActionSimCommand(state, dstState, action))
            }
        }
    }

    companion object {
        private val logger = Logger.getLogger(IntSimController::class.java)
        private const val INDENT = "    "
    }

    init {
        commands = ArrayList()
        stepSimulator = NfaStepSimulator()
        stackStates = Stack()
        backSimCommand = BackSimCommand()
    }
}