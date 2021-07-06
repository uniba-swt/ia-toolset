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
    DefenceStrategyType,
    OptionDefenceFamily,
    RefinementType,
    SimGraph,
    SimState,
    SimStateOption
} from '../../debugger/ia-data'
import { SimGraphTreeDataProvider } from './sim-graph-tree-data-provider'
import { ClearDecorationsCommand } from '../../commands/clear-decorations-command'
import { FamilyMemberRow } from './family-member-row'

export class PanelViewModel {

    implProvider: SimGraphTreeDataProvider;
    specProvider: SimGraphTreeDataProvider;
    graph: SimGraph | undefined;
    undoStack: string[] = []
    currentId: string | undefined

    autoAttack: boolean = false

    constructor(implProvider: SimGraphTreeDataProvider, specProvider: SimGraphTreeDataProvider) {
        this.implProvider = implProvider
        this.specProvider = specProvider
    }

    public reload(graph: SimGraph | undefined, autoAttack: boolean) {
        this.undoStack = []
        this.graph = graph
        this.autoAttack = autoAttack
        this.processStateId(this.graph?.initId)
    }

    public clear() {
        this.reload(undefined, this.autoAttack)
    }

    public selectFamily(arg: FamilyMemberRow) {
        // cache undo
        if (this.currentId !== undefined) {
            this.undoStack.push(this.currentId)
        }

        // choose first member
        if (!this.autoAttack) {
            this.processStateId(arg.member.dstSimStateId)
        } else {
            this.processStateId(this.findErrorDstState(arg.family) ?? arg.member.dstSimStateId)
        }
    }

    public tryBack() {
        const item = this.undoStack.pop()
        if (item !== undefined) {
            this.processStateId(item)
        } else {
            this.processStateId(this.currentId)
        }
    }

    private processStateId(id: string | undefined) {

        // process new state: clear all selection
        vscode.commands.executeCommand(ClearDecorationsCommand.Id)
        
        // process new state
        this.currentId = id
        if (this.graph !== undefined) {
            const st: SimState | undefined = this.graph.simStates.find(st => st.id === this.currentId)
            if (st !== undefined) {
                this.implProvider.initAutomaton(this.graph.impl)
                this.specProvider.initAutomaton(this.graph.spec)
                this.processState(st)
            }
        } else {
            this.implProvider.clear()
            this.specProvider.clear()
        }
    }

    private processState(state: SimState) {
        if (state.options.length === 0) {
            vscode.window.showWarningMessage('No action available')
    
            // clear
            this.implProvider.clear()
            this.specProvider.clear()
        } else {
            if (state.options.length === 1) {
                // select first option
                const opt = state.options[0]
                this.selectOption(opt.attack.refinementType, opt)
            } else {
                // if attack mode
                if (this.autoAttack) {
                    // find option that can lead to error state
                    const opt = this.findErrorOpt(state.options)
                    this.selectOption(opt.attack.refinementType, opt)
                } else {
                    // load all options
                    this.specProvider.initOptions(state.options.filter(opt => opt.attack.refinementType === RefinementType.Spec))
                    this.implProvider.initOptions(state.options.filter(opt => opt.attack.refinementType === RefinementType.Impl))
                }
            }
        }
    }

    // next step: select an option
    public selectOption(refinementType: RefinementType, opt: SimStateOption) {

        // highlight attack
        this.getProvider(opt.attack.refinementType).updateSelectedOption(opt.attack, undefined)

        // print defence
        this.getProvider(opt.defence.refinementType).updateSelectedOption(undefined, opt.defence)
    }
    
    private getProvider(refinementType: RefinementType): SimGraphTreeDataProvider {
        return refinementType === RefinementType.Spec ? this.specProvider : this.implProvider
    }

    private findErrorOpt(options: SimStateOption[]): SimStateOption {
        let cached: SimStateOption | undefined
        for (const opt of options) {
            for (const fam of opt.defence.families) {
                for (const mem of fam.members) {
                    // get dst
                    const dst = this.graph!!.simStates.find(st => st.id === mem.dstSimStateId)!!
                    if (dst.error) {
                        return opt
                    }

                    // check if having options
                    if (dst.options.length > 0) {
                        cached = opt

                        // find all option in dst
                        for (const dstOpt of dst.options) {
                            if (dstOpt.defence.strategyType !== DefenceStrategyType.Families) {
                                return opt
                            }
                        }
                    }
                }
            }
        }

        if (cached !== undefined) {
            return cached
        }

        return options[0]
    }

    private findErrorDstState(family: OptionDefenceFamily): string | undefined {
        for (const mem of family.members) {
            // get dst
            const dst = this.graph!!.simStates.find(st => st.id === mem.dstSimStateId)!!
            if (dst.error) {
                return dst.id
            }

            // check if having options
            if (dst.options.length > 0) {
                // find all option in dst
                for (const dstOpt of dst.options) {
                    if (dstOpt.defence.strategyType !== DefenceStrategyType.Families) {
                        return dst.id
                    }
                }
            }
        }

        return undefined
    }
}
