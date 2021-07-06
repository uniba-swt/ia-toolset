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
import { ShowLocationsCommand, ShowLocationsCommandArg } from '../../commands/show-location-command'
import { TextLocationItem } from '../ia-data'
import { EditorLocationSide } from '../../util/editor-util'

export interface ProductTraceRow {
    item: TextLocationItem
    side: EditorLocationSide
    highlightChild: boolean
    children: ProductTraceRow[]
}

export class ProductViewProvider implements vscode.TreeDataProvider<ProductTraceRow> {
    private items: ProductTraceRow[] = []

    private _onDidChangeTreeData: vscode.EventEmitter<any> = new vscode.EventEmitter<any>()

    readonly onDidChangeTreeData: vscode.Event<any> = this._onDidChangeTreeData.event

    public refresh(items: ProductTraceRow[]) {
        this.items = items
        this._onDidChangeTreeData.fire(undefined)
    }

    // eslint-disable-next-line no-undef
    getTreeItem(element: ProductTraceRow): vscode.TreeItem | Thenable<vscode.TreeItem> {
        const state = element.children.length > 0 ? vscode.TreeItemCollapsibleState.Collapsed : vscode.TreeItemCollapsibleState.None
        return <vscode.TreeItem> {
            label: element.item.text,
            collapsibleState: state,
            command: <vscode.Command> {
                command: ShowLocationsCommand.Id,
                title: 'Show in code editor',
                arguments: [<ShowLocationsCommandArg> {
                    row: element
                }]
            }
        }
    }

    getChildren(element?: ProductTraceRow): vscode.ProviderResult<ProductTraceRow[]> {
        if (element === undefined) {
            return this.items
        }

        return element.children
    }
}

let productProvider: ProductViewProvider

export function onNewProductTrace(items: TextLocationItem[]) {

    // convert to rows
    const rows: ProductTraceRow[] = items.map(it =>
        <ProductTraceRow> {
            item: it,
            side: EditorLocationSide.Default,
            highlightChild: true,
            children: it.children.map((child, index) =>
                convertToChild(child, index === 0 ? EditorLocationSide.Left : EditorLocationSide.Right)
            )
        }
    )

    productProvider.refresh(rows)
}

function convertToChild(item: TextLocationItem, side: EditorLocationSide): ProductTraceRow {
    return <ProductTraceRow> {
        item: item,
        side: side,
        highlightChild: false,
        children: item.children.map(it => convertToChild(it, side))
    }
}

export function onNewProductEnded() {
    ShowLocationsCommand.executeClear()
    productProvider.refresh([])
}

export function initProductView(context: vscode.ExtensionContext) {
    productProvider = new ProductViewProvider()
    context.subscriptions.push(vscode.window.registerTreeDataProvider('ia-product-view', productProvider))
    productProvider.refresh([])
}
