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

import com.google.inject.Inject
import ialib.iam.MemDotRenderer
import ialib.mia.ModalDotRenderer
import org.eclipse.xtext.generator.JavaIoFileSystemAccess
import org.eclipse.xtext.resource.SaveOptions
import org.eclipse.xtext.serializer.impl.Serializer
import swtia.ia.GModel
import swtia.startup.OperatorOptions
import swtia.sys.iam.SysIa
import swtia.sys.mia.MiaSysIa
import swtia.sys.models.SysIaBase
import swtia.transformation.iam.cfg.IamCfg
import swtia.transformation.iam.cfg.IamCfgDotBuilder
import swtia.transformation.mia.cfg.MiaCfg
import swtia.transformation.mia.cfg.MiaCfgDotBuilder
import java.nio.file.Paths

class XtextModelHelper {

    @Inject
    private lateinit var fsa: JavaIoFileSystemAccess

    @Inject
    private lateinit var serializer: Serializer

    fun dumpModel(model: GModel, suffix: String) {
        if (shouldDump()) {
            dumpToFile(serializer.serialize(model, SaveOptions.newBuilder().format().options), "__debug_1_ir_$suffix.ia", true)
        }
    }

    fun dumpCfg(cfg: IamCfg, prefix: String = "debug") {
        if (shouldDump()) {
            val name = "__${prefix}_2_cfg_${cfg.name}.dot"
            dumpToFile(IamCfgDotBuilder.toDot(cfg), name, true)
        }
    }

    fun dumpCfg(cfg: MiaCfg, prefix: String = "debug") {
        if (shouldDump()) {
            val name = "__${prefix}_2_cfg_${cfg.name}.dot"
            dumpToFile(MiaCfgDotBuilder.toDot(cfg), name, true)
        }
    }

    fun dumpIam(ia: SysIa, prefix: String = "debug") {
        if (shouldDump()) {
            val name = "__${prefix}_3_iam_${ia.name}.dot"
            dumpToFile(MemDotRenderer().render(ia.automaton), name, true)
        }
    }

    fun dumpMia(ia: MiaSysIa, prefix: String = "debug") {
        if (shouldDump()) {
            val name = "__${prefix}_3_mia_${ia.name}.dot"
            dumpToFile(ModalDotRenderer().render(ia.automaton), name, true)
        }
    }

    fun <S: SysIaBase> renderSys(sys: S) {
        val name = "runtime_${sys.name}.dot"
        when (sys) {
            is SysIa -> dumpToFile(MemDotRenderer().render(sys.automaton), name, false)
            is MiaSysIa -> dumpToFile(ModalDotRenderer().render(sys.automaton), name, false)
        }
    }

    private fun debugPath() = Paths.get(OperatorOptions.outputPath, "debug").toString()

    private fun shouldDump() = Ulogger.isVerboseOrDebug

    private fun dumpToFile(text: String, filename: String, isDebug: Boolean) {
        val path = if (isDebug) { debugPath() } else { OperatorOptions.outputPath }
        fsa.setOutputPath(path)
        fsa.generateFile(filename, text)
        Ulogger.debug { "dump to '$filename'" }
    }
}