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

import { RefinementRow } from './refinement-row'
import {
    FamilyMember,
    OptionDefenceFamily,
    RefinementType,
    TransitionJsonModel
} from '../../debugger/ia-data'
import { SimGraphTreeDataProvider } from './sim-graph-tree-data-provider'
import * as vscode from 'vscode'
import { ShowTransitionCommand } from '../../commands/show-transition-command'
import { TextLocationItemRow } from './text-location-item-row'

export class FamilyMemberRow implements RefinementRow {

    family: OptionDefenceFamily
    member: FamilyMember
    refinementType: RefinementType

    constructor(family: OptionDefenceFamily, member: FamilyMember, refinementType: RefinementType) {
        this.family = family
        this.member = member
        this.refinementType = refinementType
    }

    getTreeItem(provider: SimGraphTreeDataProvider): vscode.TreeItem {
        const tran = this.getTran(provider)
        return <vscode.TreeItem>{
            label: tran.textLocation.text,
            collapsibleState: vscode.TreeItemCollapsibleState.None,
            iconPath: new vscode.ThemeIcon('symbol-interface'),
            contextValue: 'FamilyRefinementRow',
            command: <vscode.Command>{
                command: ShowTransitionCommand.Id,
                title: 'Show in code editor',
                arguments: [tran.textLocation, this.refinementType]
            }
        }
    }

    getChildren(provider: SimGraphTreeDataProvider): RefinementRow[] {
        return this.getTran(provider).textLocation.children.map(ts =>
            new TextLocationItemRow(ts, this.refinementType)
        )
    }

    private getTran(provider: SimGraphTreeDataProvider): TransitionJsonModel {
        return provider.getTran(this.member.transitionId)!!
    }
}
