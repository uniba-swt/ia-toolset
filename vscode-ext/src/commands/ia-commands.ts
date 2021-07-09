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

import { RefinementType, TextLocationItem } from '../debugger/ia-data'
import * as vscode from 'vscode'
import { ShowTransitionCommand } from './show-transition-command'
import { ShowFamilyCommand } from './show-family-command'
import { ListTransitions } from '../lsp/refinement-tree/refinement-row'
import { AcceptFamilyCommand } from './accept-family-command'
import { BackRefinementCommand } from './back-refinement-command'
import { ClearDecorationsCommand } from './clear-decorations-command'
import { SimstateOptionCommand, SimStateOptionCommandArg } from './simstate-option-command'
import { FamilyMemberRow } from '../lsp/refinement-tree/family-member-row'
import { DebugCommand } from './debug-command'
import { ExploreProcCommand } from './explore-proc-command'
import { RunCommand } from './run-command'
import { ReloadIdeCommand } from './reload-ide-command'
import { SimulateRefinementCommand } from './simulate-refinement-command'
import { SelectTransitionCommand } from './select-transition-command'
import { ResetExploreProcessCommand } from './reset-explore-process-command'
import { SimulateErrorRefinementCommand } from './simulate-error-refinement-command'
import { ShowLocationsCommand } from './show-location-command'

export function registerCommands(context: vscode.ExtensionContext) {

    context.subscriptions.push(vscode.commands.registerCommand(
        ShowTransitionCommand.Id,
        async (args: TextLocationItem, refinementType: RefinementType) => await new ShowTransitionCommand().executeAsync(args, refinementType)))

    context.subscriptions.push(vscode.commands.registerCommand(
        ShowFamilyCommand.Id,
        async (args: ListTransitions, refinementType: RefinementType) => await new ShowFamilyCommand().executeAsync(args, refinementType)))

    context.subscriptions.push(vscode.commands.registerCommand(
        AcceptFamilyCommand.Id,
        (arg: FamilyMemberRow) => AcceptFamilyCommand.execute(arg)))

    context.subscriptions.push(vscode.commands.registerCommand(
        BackRefinementCommand.Id,
        () => new BackRefinementCommand().execute()))

    context.subscriptions.push(vscode.commands.registerCommand(
        ClearDecorationsCommand.Id,
        () => ClearDecorationsCommand.execute()))

    context.subscriptions.push(vscode.commands.registerCommand(
        SimstateOptionCommand.Id,
        (arg: SimStateOptionCommandArg) => SimstateOptionCommand.execute(arg)))

    context.subscriptions.push(vscode.commands.registerCommand(
        DebugCommand.Id,
        () => DebugCommand.execute()))

    context.subscriptions.push(vscode.commands.registerCommand(
        ExploreProcCommand.Id,
        async (uri: string, name: string) => await ExploreProcCommand.executeAsync(uri, name)))

    context.subscriptions.push(vscode.commands.registerCommand(
        RunCommand.Id,
        async (arg) => await RunCommand.executeAsync(arg)))

    context.subscriptions.push(vscode.commands.registerCommand(
        ReloadIdeCommand.Id,
        async () => await ReloadIdeCommand.execute()))

    context.subscriptions.push(vscode.commands.registerCommand(
        SimulateRefinementCommand.Id,
        async (arg) => await SimulateRefinementCommand.executeAsync(arg)))

    context.subscriptions.push(vscode.commands.registerCommand(
        SimulateErrorRefinementCommand.Id,
        async (arg) => await SimulateErrorRefinementCommand.executeAsync(arg)))

    context.subscriptions.push(vscode.commands.registerCommand(
        SelectTransitionCommand.Id,
        async (arg) => await SelectTransitionCommand.executeAsync(arg)))

    context.subscriptions.push(vscode.commands.registerCommand(
        ResetExploreProcessCommand.Id,
        () => ResetExploreProcessCommand.execute()))

    context.subscriptions.push(vscode.commands.registerCommand(
        ShowLocationsCommand.Id,
        async (arg: any) => await ShowLocationsCommand.executeAsync(arg)))
}
