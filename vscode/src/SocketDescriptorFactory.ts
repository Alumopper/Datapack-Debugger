/*********************************************************************
 * Copyright (c) 2023 Arm Limited and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *********************************************************************/

/** Code from https://github.com/eclipse-cdt-cloud/vscode-websocket-adapter */

import * as vscode from 'vscode';
import { SocketDebugSession } from './SocketDebugSession';

export class SocketDescriptorFactory implements vscode.DebugAdapterDescriptorFactory {
    public async activate(context: vscode.ExtensionContext): Promise<void> {
        context.subscriptions.push(
            vscode.debug.registerDebugAdapterDescriptorFactory('sniffer', this)
        );
    }

    public async createDebugAdapterDescriptor(session: vscode.DebugSession, _executable: vscode.DebugAdapterExecutable | undefined): Promise<vscode.DebugAdapterDescriptor | undefined> {
        // Get the server address from configuration
        const serverAddress = session.configuration.address;
        
        if (!serverAddress) {
            throw new Error('No server address provided. Please specify the "address" property in your launch configuration.');
        }
        
        console.log('Connecting to server at:', serverAddress);
        
        // Create inline adapter
        const socketSession = new SocketDebugSession(serverAddress, session.configuration.pathMapping);
        return new vscode.DebugAdapterInlineImplementation(socketSession);
    }
}