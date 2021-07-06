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

// common
export interface MLocation {
    lineBegin: number;
    lineEnd: number;
    colBegin: number;
    colEnd: number;
}

export interface TextLocationItem {
    id: string,
    text: string,
    locations: MLocation[]
    children: TextLocationItem[]
}

export enum RefinementType {
    // eslint-disable-next-line no-unused-vars
    Spec = 0,
    // eslint-disable-next-line no-unused-vars
    Impl = 1
}

export enum DefenceStrategyType {
    // eslint-disable-next-line no-unused-vars
    ErrorNoAction = 0,
    // eslint-disable-next-line no-unused-vars
    ErrorNoFamily = 1,
    // eslint-disable-next-line no-unused-vars
    Families = 2
}

export interface FamilyMember {
    transitionId: string,
    dstSimStateId: string
}

export interface OptionDefenceFamily {
    members: FamilyMember[]
}

export interface OptionAttack {
    refinementType: RefinementType
    transitionId: string
}

export interface OptionDefence {
    refinementType: RefinementType
    strategyType: DefenceStrategyType
    families: OptionDefenceFamily[]
}

export interface SimStateOption {
    attack: OptionAttack
    defence: OptionDefence
}

export interface SimState {
    id: string
    error: boolean
    options: SimStateOption[]
}

export interface TransitionJsonModel {
    id: string
    dstId: string
    textLocation: TextLocationItem
}

export interface StateJsonModel {
    id: string
    transitionIds: string[]
}

export interface Automaton {
    name: string,
    initId: string,
    states: StateJsonModel[],
    transitions: TransitionJsonModel[]
}

export interface SimGraph {
    spec: Automaton
    impl: Automaton
    simStates: SimState[]
    initId: string
}
