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
import {
    DecorationRangeBehavior,
    DecorationRenderOptions,
    OverviewRulerLane,
    TextEditorDecorationType,
    TextEditorRevealType
} from 'vscode'
import { RefinementType, TextLocationItem } from '../debugger/ia-data'
import { editorManager } from '../lsp/refinement-tree/refinement-view'
import { CommandUtil } from './command-util'
import { EditorUtil } from '../util/editor-util'

const defaultDecorType: TextEditorDecorationType = createDecor()

export class ShowTransitionCommand {

    public static Id = 'ia-toolset.cmdShowTransition'

    public async executeAsync(item: TextLocationItem, refinementType: RefinementType) {
        // show in code editor
        const editors = await editorManager.getEditors()
        const editor = refinementType === RefinementType.Impl ? editors[0] : editors[1]
        const ranges = CommandUtil.getRangesOf(item)!!
        editor.setDecorations(defaultDecorType, ranges)
        if (ranges.length > 0) {
            editor.revealRange(ranges[0], TextEditorRevealType.InCenter)
        }
    }

    public static executeClear() {
        EditorUtil.clearDecoration(defaultDecorType)
    }
}

function createDecor(): TextEditorDecorationType {
    return vscode.window.createTextEditorDecorationType(<DecorationRenderOptions>{
        rangeBehavior: DecorationRangeBehavior.ClosedClosed,
        isWholeLine: false,
        backgroundColor: new vscode.ThemeColor('peekViewEditor.matchHighlightBackground'),
        color: new vscode.ThemeColor('peekViewResult.selectionForeground'),
        borderColor: new vscode.ThemeColor('peekViewEditor.matchHighlightBorder'),
        borderWidth: '1px',
        borderStyle: 'solid',
        overviewRulerLane: OverviewRulerLane.Center,
        overviewRulerColor: new vscode.ThemeColor('peekViewEditor.matchHighlightBackground')
    })
}
