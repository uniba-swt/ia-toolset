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

export enum EditorLocationSide {
    Default,
    Left,
    Right
}

export class EditorUtil {

    public static async openDocAsync(path: string): Promise<vscode.TextEditor> {
        const uri = vscode.Uri.file(path)
        return vscode.window.showTextDocument(uri, <vscode.TextDocumentShowOptions>{
            viewColumn: vscode.ViewColumn.Beside,
            preserveFocus: true
        })
    }

    public static clearDecoration(decorType: vscode.TextEditorDecorationType) {
        for (const editor of vscode.window.visibleTextEditors) {
            editor.setDecorations(decorType, [])
        }
    }

    public static async getEditorAsync(path: string, side: EditorLocationSide): Promise<vscode.TextEditor> {
        if (side === EditorLocationSide.Default) {
            return vscode.window.activeTextEditor!!
        }

        const editors = await EditorUtil.getEditors(path)
        return side === EditorLocationSide.Left ? editors[0] : editors[1]
    }

    public static async getEditors(path: string): Promise<[vscode.TextEditor, vscode.TextEditor]> {
        const items = vscode.window.visibleTextEditors
            .filter(ed => ed.document.uri.path === path)
            .sort((ed1, ed2) => ed1.viewColumn!! < ed2.viewColumn!! ? -1 : (ed1.viewColumn!! > ed2.viewColumn!! ? 1 : 0))
        const left = await EditorUtil.getOrOpen(path, items.length >= 1 ? items[0] : undefined)
        const right = await EditorUtil.getOrOpen(path, items.length >= 2 ? items[1] : undefined)
        return [left, right]
    }

    private static async getOrOpen(path: string, input: vscode.TextEditor | undefined): Promise<vscode.TextEditor> {
        if (input !== undefined && input.document.uri.path === path) {
            return input
        }

        // open new if not matched
        return await EditorUtil.openDocAsync(path)
    }

    public static getDefaultPath(): string | undefined {
        return vscode.window.activeTextEditor?.document?.uri?.path
    }
}
