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

import { LanguageClient, LanguageClientOptions, ServerOptions, StreamInfo } from 'vscode-languageclient'
import * as net from 'net'
import { ExtensionContext } from 'vscode'
import { BinBundleUtil } from '../util/bin-bundle-util'

export const ServerName = 'IA IDE Server'

/**
 * Ultility class for handling language client
 */
export class LanguageClientUtil {

    /**
     * create LanguageClient that works with embedded BahnDSL language server binary
     * @param context context
     * @returns lc
     */
    static createEmbeddedClient(context: ExtensionContext): LanguageClient {
        const script = BinBundleUtil.getServerPath(context)
        const serverOptions: ServerOptions = {
            run: {
                command: script, args: ['-lsp', '-trace']
            },
            debug: {
                command: script,
                args: ['-lsp', '-log', '-trace']
            }
        }

        return new LanguageClient(ServerName, serverOptions, this.createClientOptions())
    }

    /**
     * create LanguageClient that works with remote LSP (used for debugging)
     * @param port port
     * @returns lc
     */
    static createRemoteClient(port: number): LanguageClient {

        const serverInfo = () => {
            const socket = net.connect({ port: port })
            const result: StreamInfo = {
                writer: socket,
                reader: socket
            }
            return Promise.resolve(result)
        }

        return new LanguageClient(ServerName, serverInfo, this.createClientOptions())
    }

    /**
     * create client options
     * @returns options
     */
    private static createClientOptions(): LanguageClientOptions {
        return {
            documentSelector: [{
                scheme: 'file',
                language: 'ia'
            }]
        }
    }
}
