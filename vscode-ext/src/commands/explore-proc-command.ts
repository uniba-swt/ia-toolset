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

import { lsp } from '../lsp/ia-lsp'
import {
    ExploreProcRequest,
    ExploreProcResponse,
    IaEndpoints
} from '../lsp/lsp-messages'
import * as vscode from 'vscode'
import { runProcess } from '../lsp/explore-process/explore-process-view'

export class ExploreProcCommand {
    public static Id = 'ia-toolset.cmdExploreProc'

    public static async executeAsync(uri: string, procName: string) {
        if (!lsp.ensureLspIsReady()) {
            return
        }

        lsp.updateActionBar('Explore process')
        try {
            const res = await lsp.lc.sendRequest<ExploreProcResponse>(IaEndpoints.ExploreProc, <ExploreProcRequest> { uri: uri, procName: procName }, undefined)
            lsp.hideActionBar()
            ExploreProcCommand.processResponse(res)
        } catch (error) {
            console.log(error)
        }
    }

    private static processResponse(res: ExploreProcResponse) {
        // check result
        if (res.errorMsg !== undefined || res.data === undefined) {
            vscode.window.showErrorMessage(res.errorMsg ?? 'Failed to explore the process')
            return
        }

        // process the data
        runProcess(res.data)
    }
}
