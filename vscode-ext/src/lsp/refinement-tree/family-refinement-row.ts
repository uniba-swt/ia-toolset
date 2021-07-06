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

import { OptionDefenceFamily, RefinementType } from '../../debugger/ia-data'
import { SimGraphTreeDataProvider } from './sim-graph-tree-data-provider'
import * as vscode from 'vscode'
import { ShowFamilyCommand } from '../../commands/show-family-command'
import { ListTransitions, RefinementRow } from './refinement-row'
import { FamilyMemberRow } from './family-member-row'

export class FamilyRefinementRow implements RefinementRow {
    family: OptionDefenceFamily
    order: number
    refinementType: RefinementType

    constructor(family: OptionDefenceFamily, order: number, refinementType: RefinementType) {
        this.family = family
        this.order = order
        this.refinementType = refinementType
    }

    getTreeItem(provider: SimGraphTreeDataProvider): vscode.TreeItem {
        const arg = <ListTransitions>{
            items: this.family.members.map(mb => provider.getTran(mb.transitionId))
        }
        return <vscode.TreeItem>{
            label: `Family ${this.order}`,
            collapsibleState: vscode.TreeItemCollapsibleState.Expanded,
            iconPath: new vscode.ThemeIcon('group-by-ref-type'),
            command: <vscode.Command>{
                command: ShowFamilyCommand.Id,
                title: 'Show in code editor',
                arguments: [arg, this.refinementType]
            }
        }
    }

    getChildren(provider: SimGraphTreeDataProvider): RefinementRow[] {
        return this.family.members.map(mb =>
            new FamilyMemberRow(this.family, mb, this.refinementType)
        )
    }
}
