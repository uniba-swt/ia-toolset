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

import { RefinementType } from '../debugger/ia-data'
import { ListTransitions } from '../lsp/refinement-tree/refinement-row'
import { editorManager } from '../lsp/refinement-tree/refinement-view'
import { CommandUtil } from './command-util'
import {
    DecorationRangeBehavior,
    DecorationRenderOptions,
    TextEditorDecorationType, TextEditorRevealType
} from 'vscode'
import * as vscode from 'vscode'
import { EditorUtil } from '../util/editor-util'

const familyDecorType: TextEditorDecorationType = createFamilyDecorType()

export class ShowFamilyCommand {

    public static Id = 'ia-toolset.cmdShowFamily'

    public async executeAsync(list: ListTransitions, refinementType: RefinementType) {
        const editors = await editorManager.getEditors()
        const editor = refinementType === RefinementType.Impl ? editors[0] : editors[1]

        // highlight every single family
        const ranges: vscode.Range[] = []
        for (const item of list.items) {
            for (const r of CommandUtil.getRangesOf(item.textLocation)) {
                ranges.push(r)
            }
        }

        editor.setDecorations(familyDecorType, ranges)
        if (ranges.length > 0) {
            editor.revealRange(ranges[0], TextEditorRevealType.InCenter)
        }
    }

    public static executeClear() {
        EditorUtil.clearDecoration(familyDecorType)
    }
}

function createFamilyDecorType(): TextEditorDecorationType {
    return vscode.window.createTextEditorDecorationType(<DecorationRenderOptions>{
        rangeBehavior: DecorationRangeBehavior.ClosedClosed,
        isWholeLine: false,
        backgroundColor: new vscode.ThemeColor('textCodeBlock.background'),
        color: new vscode.ThemeColor('textLink.activeForeground')
    })
}
