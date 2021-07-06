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
    Automaton,
    DefenceStrategyType,
    OptionAttack,
    OptionDefence,
    RefinementType,
    SimStateOption,
    TransitionJsonModel
} from '../../debugger/ia-data'
import { RefinementRow } from './refinement-row'
import { TextLocationItemRow } from './text-location-item-row'
import { FamilyRefinementRow } from './family-refinement-row'
import { OptionRefinementRow } from './option-refinement-row'

export class SimGraphTreeDataProvider implements vscode.TreeDataProvider<RefinementRow> {
    private _onDidChangeTreeData: vscode.EventEmitter<any> = new vscode.EventEmitter<any>()

    readonly onDidChangeTreeData: vscode.Event<any> = this._onDidChangeTreeData.event

    refinementType: RefinementType

    constructor(refinementType: RefinementType) {
        this.refinementType = refinementType
    }

    ia: Automaton | undefined

    // mode 1: list all attach options
    options: SimStateOption[] | undefined

    // mode 2: in the game (could be in attach or defence mode)
    optionAttack: OptionAttack | undefined

    optionDefence: OptionDefence | undefined

    public initAutomaton(ia: Automaton) {
        this.ia = ia
        this.refresh()
    }

    public initOptions(options: SimStateOption[]) {
        this.options = options
        this.optionDefence = undefined
        this.optionAttack = undefined
        this.refresh()

        // show message
        vscode.window.showInformationMessage('Select an attach option')
    }

    public updateSelectedOption(optionAttack: OptionAttack | undefined, optionDefence: OptionDefence | undefined) {
        this.optionAttack = optionAttack
        this.optionDefence = optionDefence
        this.options = undefined
        this.refresh()

        // show message
        // vscode.window.showInformationMessage('Select a defence family')
    }

    public clear() {
        this.ia = undefined
        this.options = undefined
        this.optionDefence = undefined
        this.optionAttack = undefined
        this.refresh()
    }

    private refresh() {
        this._onDidChangeTreeData.fire(undefined)
    }

    // eslint-disable-next-line no-undef
    getTreeItem(element: RefinementRow): vscode.TreeItem | Thenable<vscode.TreeItem> {
        return element.getTreeItem(this)
    }

    getChildren(element?: RefinementRow): vscode.ProviderResult<RefinementRow[]> {
        if (element === undefined) {

            // check if attach
            if (this.optionAttack !== undefined) {
                return [this.getAttackRow(this.optionAttack)]
            }

            // check if defence
            if (this.optionDefence !== undefined) {
                return this.getDefenceRows(this.optionDefence)
            }

            // list all options
            if (this.options !== undefined) {
                return this.options.map(opt =>
                    new OptionRefinementRow(this.refinementType, opt)
                )
            }

            return []
        }

        return element.getChildren(this)
    }

    public getTran(id: string): TransitionJsonModel | undefined {
        if (this.ia !== undefined) {
            return this.ia.transitions.find(ts => ts.id === id)
        }

        return undefined
    }

    /**
     * helper get option attach row
     * @param optionAttack
     * @private
     */
    private getAttackRow(optionAttack: OptionAttack): TextLocationItemRow {
        return new TextLocationItemRow(this.getTran(optionAttack.transitionId)!!.textLocation, optionAttack.refinementType)
    }

    /**
     * helper get list of option defence rows
     * @param optionDefence
     * @private
     */
    private getDefenceRows(optionDefence: OptionDefence): RefinementRow[] {
        switch (optionDefence.strategyType) {
            case DefenceStrategyType.Families:
                return optionDefence.families.map((fs, index) =>
                    new FamilyRefinementRow(fs, index + 1, optionDefence.refinementType)
                )
            case DefenceStrategyType.ErrorNoAction:
                vscode.window.showErrorMessage('No action found')
                break
            case DefenceStrategyType.ErrorNoFamily:
                vscode.window.showErrorMessage('No family found')
                break
        }

        return []
    }
}
