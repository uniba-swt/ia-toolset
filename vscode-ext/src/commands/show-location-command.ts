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
import { DecorationOptions, DecorationRangeBehavior, DecorationRenderOptions, TextEditorDecorationType } from 'vscode'
import { CommandUtil } from './command-util'
import { TextLocationItem } from '../debugger/ia-data'
import { EditorLocationSide, EditorUtil } from '../util/editor-util'
import { ProductTraceRow } from '../debugger/product/product-view'

const locationDecor: TextEditorDecorationType = createDecorType()

export interface ShowLocationsCommandArg {
    row: ProductTraceRow
}

export class ShowLocationsCommand {
    public static Id = 'ia-toolset.cmdShowLocations'

    public static async executeAsync(arg: ShowLocationsCommandArg) {
        // clear first
        EditorUtil.clearDecoration(locationDecor)

        // update
        await this.highlight(arg.row.item, arg.row.side)

        // children
        if (arg.row.highlightChild) {
            for (const child of arg.row.children) {
                await this.highlight(child.item, child.side)
            }
        }
    }

    private static async highlight(item: TextLocationItem, side: EditorLocationSide) {
        if (item.locations.length === 0) {
            return
        }

        const editor = await EditorUtil.getEditorAsync(EditorUtil.getDefaultPath()!!, side)
        const options = item.locations.map(loc =>
            <DecorationOptions> {
                range: CommandUtil.toRange(loc)!!,
                hoverMessage: item.text
            }
        )
        editor.setDecorations(locationDecor, options)
        editor.revealRange(options[0].range, vscode.TextEditorRevealType.InCenter)
    }

    public static executeClear() {
        EditorUtil.clearDecoration(locationDecor)
    }
}

function createDecorType(): TextEditorDecorationType {
    return vscode.window.createTextEditorDecorationType(<DecorationRenderOptions>{
        rangeBehavior: DecorationRangeBehavior.ClosedClosed,
        isWholeLine: false,
        backgroundColor: new vscode.ThemeColor('statusBar.debuggingBackground'),
        color: new vscode.ThemeColor('statusBar.debuggingForeground'),
        borderColor: new vscode.ThemeColor('statusBar.debuggingBorder')
    })
}
