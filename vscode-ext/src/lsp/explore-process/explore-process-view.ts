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

// context
import * as vscode from 'vscode'
import { Automaton, StateJsonModel, TextLocationItem, TransitionJsonModel } from '../../debugger/ia-data'
import { ShowLocationsCommand, ShowLocationsCommandArg } from '../../commands/show-location-command'
import { ClearDecorationsCommand } from '../../commands/clear-decorations-command'
import { EditorLocationSide } from '../../util/editor-util'
import { ProductTraceRow } from '../../debugger/product/product-view'

abstract class TransitionRowTreeProvider implements vscode.TreeDataProvider<TextLocationItem> {

    private _onDidChangeTreeData: vscode.EventEmitter<any> = new vscode.EventEmitter<any>()

    readonly onDidChangeTreeData: vscode.Event<any> = this._onDidChangeTreeData.event

    protected refresh() {
        this._onDidChangeTreeData.fire(undefined)
    }

    abstract getItems(): TransitionJsonModel[]

    getChildren(element?: TextLocationItem): vscode.ProviderResult<TextLocationItem[]> {
        if (element === undefined) {
            return this.getItems().map(ts => ts.textLocation)
        }

        return element.children
    }

    // eslint-disable-next-line no-undef
    getTreeItem(element: TextLocationItem): vscode.TreeItem | Thenable<vscode.TreeItem> {
        const state = element.children.length > 0 ? vscode.TreeItemCollapsibleState.Collapsed : vscode.TreeItemCollapsibleState.None
        return <vscode.TreeItem> {
            label: element.text,
            collapsibleState: state,
            contextValue: element.id.length > 0 ? 'TransitionRow' : undefined,
            command: <vscode.Command> {
                command: ShowLocationsCommand.Id,
                title: 'Show in code editor',
                arguments: [<ShowLocationsCommandArg> {
                    row: <ProductTraceRow> {
                        item: element,
                        side: EditorLocationSide.Default,
                        highlightChild: false,
                        children: []
                    }
                }]
            }
        }
    }
}

class AutomatonTreeProvider extends TransitionRowTreeProvider {

    trans: TransitionJsonModel[] = []

    reload(trans: TransitionJsonModel[]) {
        this.trans = trans
        this.refresh()
    }

    getItems(): TransitionJsonModel[] {
        return this.trans
    }
}

class HistoryTreeProvider extends TransitionRowTreeProvider {

    stack: TransitionJsonModel[] = []

    public push(tran: TransitionJsonModel) {
        this.stack.push(tran)
        this.refresh()
    }

    public reset() {
        this.stack = []
        this.refresh()
    }

    getItems(): TransitionJsonModel[] {
        return this.stack.reverse()
    }
}

class ExploreProcessViewModel {

    iaProvider: AutomatonTreeProvider
    historyProvider: HistoryTreeProvider
    ia!: Automaton

    constructor(iaProvider: AutomatonTreeProvider, historyProvider: HistoryTreeProvider) {
        this.iaProvider = iaProvider
        this.historyProvider = historyProvider
    }

    public reload(ia: Automaton) {
        this.ia = ia
        this.reset()
    }

    public reset() {
        // clear history
        this.historyProvider.reset()

        // reload
        this.updateCurrentState(this.getState(this.ia.initId))
    }

    public selectTransition(id: string) {
        // push to history
        const tran = this.getTran(id)
        this.historyProvider.push(tran)

        // reload
        this.updateCurrentState(this.getState(tran.dstId))
    }

    private updateCurrentState(st: StateJsonModel) {
        // clear
        ClearDecorationsCommand.execute()

        // get all transitions
        const trans = st.transitionIds.map(id => this.getTran(id))
        this.iaProvider.reload(trans)
    }

    private getState(id: string): StateJsonModel {
        return this.ia.states.find(s => s.id === id)!!
    }

    private getTran(id: string): TransitionJsonModel {
        return this.ia.transitions.find(ts => ts.id === id)!!
    }
}

export let exploreProcessVm: ExploreProcessViewModel

export function runProcess(ia: Automaton) {
    exploreProcessVm.reload(ia)
}

export function activateProcessView(context: vscode.ExtensionContext) {
    const iaProvider = new AutomatonTreeProvider()
    const historyProvider = new HistoryTreeProvider()
    exploreProcessVm = new ExploreProcessViewModel(iaProvider, historyProvider)
    context.subscriptions.push(vscode.window.registerTreeDataProvider('ia-explore-process', iaProvider))
    context.subscriptions.push(vscode.window.registerTreeDataProvider('ia-explore-history', historyProvider))
}
