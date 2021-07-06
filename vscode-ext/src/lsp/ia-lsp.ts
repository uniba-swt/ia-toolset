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

import { Trace } from 'vscode-jsonrpc'
import * as vscode from 'vscode'
import { LanguageClient, State } from 'vscode-languageclient'
import { LanguageClientUtil, ServerName } from './language-client-util'
import { IaConfigUtil } from '../util/ia-config'

/**
 * main vscode extension implementation
 */
export class LspManager {

    private _context!: vscode.ExtensionContext
    private _lc!: LanguageClient
    private readonly _statusBar: vscode.StatusBarItem
    private readonly _statusBarAction: vscode.StatusBarItem

    private _lcState: State = State.Stopped

    constructor() {
        this._statusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 0)
        this._statusBarAction = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 1)
    }

    public init(context: vscode.ExtensionContext) {
        this._context = context

        // create status bar
        this.pushDispose(this._statusBar)
        this.pushDispose(this._statusBarAction)
    }

    /**
     * active the LC
     */
    public activate() {
        console.log('LspManager -> activate')
        this.startLanguageClient()
    }

    get lc(): LanguageClient {
        return this._lc
    }

    get lcState(): State {
        return this._lcState
    }

    /**
     * push for clean up
     * @param dispose Disposable
     */
    private pushDispose(dispose: vscode.Disposable) {
        this._context.subscriptions.push(dispose)
    }

    /**
     * create LC
     * @returns LC
     */
    private createLanguageClient(): LanguageClient {

        const config = IaConfigUtil.loadConfig()
        if (config.remoteLspEnabled) {
            console.log('connect to remote LSP server, port: ' + config.remoteLspPort)
            return LanguageClientUtil.createRemoteClient(config.remoteLspPort)    
        } else {
            console.log('launch embedded LSP server')
            return LanguageClientUtil.createEmbeddedClient(this._context)
        }
    }

    /**
     * Start connecting to LC
     */
    public startLanguageClient() {
        // create lc
        this._lc = this.createLanguageClient()
        this._lc.trace = Trace.Verbose
        this._lc.onReady().then(() => {
            this.updateStatus(`$(pass) ${ServerName}`)
        })

        // event
        this._lc.onDidChangeState(evt => {
            this._lcState = evt.newState
        })

        // update status
        this.updateStatus(`$(sync~spin) Loading ${ServerName}...`)
        this.pushDispose(this._lc.start())
    }

    public ensureLspIsReady(): boolean {
        if (this._lc.needsStart()) {
            this.showActionError(`${ServerName} is not ready, try command 'Reload IA IDE Server'`)
            return false
        }

        if (this._lcState === State.Starting) {
            vscode.window.showInformationMessage(`${ServerName} is starting, try again later`)
            return false
        }

        return true
    }

    public static getCurrentUri(): string | undefined {
        return vscode.window.activeTextEditor?.document.fileName ?? undefined
    }

    private showActionError(err: any) {
        this.hideActionBar()
        vscode.window.showErrorMessage(err)
    }

    public updateActionBar(text: string) {
        this._statusBarAction.text = `$(sync~spin) ${text}`
        this._statusBarAction.show()
    }

    public hideActionBar() {
        this._statusBarAction.text = ''
        this._statusBarAction.hide()
    }

    private updateStatus(text: string) {
        this._statusBar.text = text
        this._statusBar.show()
    }

    /**
     * deactivate the LC
     * @returns promise
     */
    deactivate(): Promise<void> {
        console.log('IaVsCodeExtension -> deactivate')
        return this._lc.stop()
    }
}

export const lsp = new LspManager()
