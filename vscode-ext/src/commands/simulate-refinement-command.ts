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

import { lsp, LspManager } from '../lsp/ia-lsp'
import {
    IaEndpoints,
    SimulateRefineRequest,
    SimulateRefineResponse
} from '../lsp/lsp-messages'
import * as vscode from 'vscode'
import { runSimulation } from '../lsp/refinement-tree/refinement-view'

export class SimulateRefinementCommand {

    public static Id = 'ia-toolset.cmdSimulateRefinement'

    public static async executeAsync(uri: string | undefined) {
        if (!lsp.ensureLspIsReady()) {
            return
        }

        const path = uri ?? LspManager.getCurrentUri()
        if (path === undefined) {
            return
        }

        lsp.updateActionBar('Simulating')
        try {
            const res = await lsp.lc.sendRequest<SimulateRefineResponse>(IaEndpoints.SimulateRefinement, <SimulateRefineRequest> { uri: path }, undefined)
            lsp.hideActionBar()
            SimulateRefinementCommand.processResponse(path, res)
        } catch (error) {
            console.log(error)
        }
    }

    private static processResponse(path: string, res: SimulateRefineResponse) {
        // check result
        if (res.errorMsg !== undefined) {
            if (res.data === undefined) {
                vscode.window.showErrorMessage(res.errorMsg)
            } else {
                vscode.window.showWarningMessage(res.errorMsg)
            }
        }

        // stop if having no json data
        if (res.data === undefined) { return }

        // check if simulation
        runSimulation(res.data, path)
    }
}
