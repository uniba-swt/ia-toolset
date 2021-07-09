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

import { ExtensionContext } from 'vscode'
import * as cp from 'child_process'
import * as os from 'os'
import * as path from 'path'
import * as vscode from 'vscode'

interface ToolInfo {
    name: string,
    cmd: string,
    installLink: string
}

export class BinBundleUtil {
    /**
     * get server path
     * @param context context
     * @returns path
     */
    public static getServerPath(context: ExtensionContext): string {
        const name = 'ia-ide-server'
        const launcher = os.platform() === 'win32' ? (name + '.bat') : name
        return context.asAbsolutePath(path.join('lsp-server', 'bin', launcher))
    }

    private static tools: ToolInfo[] = [
        <ToolInfo>{
            name: 'Java 11',
            cmd: 'java1 --version',
            installLink: 'https://adoptopenjdk.net/?variant=openjdk11&jvmVariant=hotspot'
        },
        <ToolInfo>{
            name: 'Z3 Theorem Prover',
            cmd: 'z3 --version',
            installLink: 'https://github.com/Z3Prover/z3/releases'
        },
        <ToolInfo>{
            name: 'mCRL2',
            cmd: 'pbes2bes --version && pbessolve --version',
            installLink: 'https://github.com/mCRL2org/mCRL2/releases'
        }
    ]
    
    public static ensureToolsAreInstalled() {

        const optInstall = 'Install'
        for (const tool of BinBundleUtil.tools) {
            cp.exec(tool.cmd, (error, stdout) => {
                const valid = error === null && stdout !== null && stdout.length > 0
                if (!valid) {
                    // ask to install
                    vscode.window.showErrorMessage(`${tool.name} is missing (check the PATH env variable)`, optInstall)
                        .then(selection => {
                            console.log(selection)
                            if (selection === optInstall) {
                                vscode.env.openExternal(vscode.Uri.parse(tool.installLink))
                            }
                        })
                }
            })
        }
    }
}
