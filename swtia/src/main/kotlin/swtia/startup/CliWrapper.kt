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

package swtia.startup

import org.apache.commons.cli.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class CliWrapper {
    private val options = createOptions()

    fun parse(args: Array<String>?): Boolean {
        val cmdline = parseCmdLine(args) ?: return false

        // stop if asking for help
        if (cmdline.hasOption("h")) {
            showHelp()
            return false
        }

        if (cmdline.argList.isEmpty()) {
            showErrorAndExit("missing input file")
            return false
        }

        cmdline.argList.forEach { f ->
            if (!Files.exists(Paths.get(f))) {
                showErrorAndExit("file is not exist: $f")
                return false
            }
        }

        OperatorOptions.update(
            cmdline.hasOption('d'),
            cmdline.hasOption('v'),
            cmdline.getOptionValue('o', OperatorOptions.outputPath),
            cmdline.hasOption(KeyFormat),
            cmdline.argList
        )
        return true
    }

    private fun parseCmdLine(args: Array<String>?): CommandLine? {
        // create options and parse
        val parser = DefaultParser()
        return try {
            parser.parse(options, args)
        } catch (e: ParseException) {
            showErrorAndExit(e.message ?: "")
            return null
        }
    }

    private fun showErrorAndExit(msg: String) {
        System.err.println(msg)
        System.err.println()
        showHelp()
        exitProcess(1)
    }

    private fun showHelp() {
        val formatter = HelpFormatter()
        println("IA-Toolset $Version")
        println()
        formatter.printHelp(
            "iac [OPTIONS] <file1> [file2]",
            null,
            options, null
        )
    }

    companion object {

        // should find another solution to sync the version with build.gradle and show git commit reference
        private const val Version = "1.0.1"

        private const val KeyFormat = "fmt"

        private fun createOptions(): Options {
            val options = Options()
            options.addOption("o", "output", false, "path for generated output")
            options.addOption("v", "verbose", false, "verbose output")
            options.addOption("d", "debug", false, "show debug log")
            options.addOption("h", "help", false, "show this help menu")
            options.addOption(Option.builder().longOpt(KeyFormat).hasArg(false).desc("format source code").build())
            return options
        }
    }
}