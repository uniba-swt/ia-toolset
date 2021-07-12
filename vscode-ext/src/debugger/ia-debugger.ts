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

import * as vscode from 'vscode'
import { onNewProductEnded, onNewProductTrace } from './product/product-view'
import { TextLocationItem } from './ia-data'
import { IaConfigUtil } from '../util/ia-config'
import { DebugUtil } from './debug-util'
import { BinBundleUtil } from '../util/bin-bundle-util'
import { DebugAdapterExecutableOptions } from 'vscode'
import { ClearDecorationsCommand } from '../commands/clear-decorations-command'

class IAFactory implements vscode.DebugAdapterDescriptorFactory {
    private readonly ctx: vscode.ExtensionContext
    constructor(ctx: vscode.ExtensionContext) {
        this.ctx = ctx
    }

    createDebugAdapterDescriptor(session: vscode.DebugSession, executable: vscode.DebugAdapterExecutable | undefined): vscode.ProviderResult<vscode.DebugAdapterDescriptor> {
        // run remote server
        const config = IaConfigUtil.loadConfig()
        if (config.remoteDebugAdapterEnabled) {
            return new vscode.DebugAdapterServer(config.remoteDebugAdapterPort, '0.0.0.0')
        } else {
            const command = BinBundleUtil.getServerPath(this.ctx)
            const args = ['-debug']
            const opt = <DebugAdapterExecutableOptions> {
                cwd: IAFactory.getCurrentPwd()
            }
            return new vscode.DebugAdapterExecutable(command, args, opt)
        }
    }

    private static getCurrentPwd(): string | undefined {
        const items = vscode.workspace.workspaceFolders
        if (items !== undefined && items.length > 0) {
            const wp = items[0]
            return wp.uri.fsPath
        }

        return undefined
    }
}

class IADebugConfigurationProvider implements vscode.DebugConfigurationProvider {
    resolveDebugConfiguration?(folder: vscode.WorkspaceFolder | undefined, debugConfiguration: vscode.DebugConfiguration, token?: vscode.CancellationToken): vscode.ProviderResult<vscode.DebugConfiguration> {

        // load default
        const def = DebugUtil.createConfiguration()

        // update
        debugConfiguration.type = debugConfiguration.type ?? def.type
        debugConfiguration.name = debugConfiguration.name ?? def.name
        debugConfiguration.request = debugConfiguration.request ?? def.request
        debugConfiguration.program = debugConfiguration.program ?? def.program
        debugConfiguration.stopOnEntry = debugConfiguration.stopOnEntry ?? def.stopOnEntry
        return debugConfiguration
    }
}

class IADebugAdapterTracker implements vscode.DebugAdapterTracker {
    
    session: vscode.DebugSession
    constructor(session: vscode.DebugSession) {
        this.session = session
    }

    onDidSendMessage(message: any): void {
        console.log('onDidSendMessage')
        console.log(message)
    }

    onWillReceiveMessage(message: any): void {
        console.log('onWillReceiveMessage')
        console.log(message)
    }
}

class IADebugAdapterTrackerFactory implements vscode.DebugAdapterTrackerFactory {
    createDebugAdapterTracker(session: vscode.DebugSession): vscode.ProviderResult<vscode.DebugAdapterTracker> {
        return new IADebugAdapterTracker(session)
    }
}

export const DebuggerType = 'ia-debugger'

export function initDebugger(context: vscode.ExtensionContext) {
    context.subscriptions.push(vscode.debug.registerDebugAdapterDescriptorFactory(DebuggerType, new IAFactory(context)))
    context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider(DebuggerType, new IADebugConfigurationProvider()))
    context.subscriptions.push(vscode.debug.registerDebugAdapterTrackerFactory(DebuggerType, new IADebugAdapterTrackerFactory()))
    vscode.debug.onDidStartDebugSession(evt => {
        console.log('onDidStartDebugSession')
        onNewProductTrace([])
        console.log(evt)
    })
    vscode.debug.onDidReceiveDebugSessionCustomEvent(async (evt) => {
        const name = evt.event
        console.log('onDidReceiveDebugSessionCustomEvent: ' + name)
        switch (name) {
            case 'productTraces':
                onNewProductTrace(evt.body.items as TextLocationItem[])
                break
            case 'productEnd':
                onNewProductEnded()
                break
        }
    })
    vscode.debug.onDidTerminateDebugSession((_) => {
        ClearDecorationsCommand.execute()
    })
}   
