import * as vscode from 'vscode';
import { SocketDebugSession } from './SocketDebugSession';

export class SocketDescriptorFactory implements vscode.DebugAdapterDescriptorFactory {
    public async activate(context: vscode.ExtensionContext): Promise<void> {
        context.subscriptions.push(
            vscode.debug.registerDebugAdapterDescriptorFactory('websocket-debug', this)
        );
    }

    public async createDebugAdapterDescriptor(session: vscode.DebugSession, _executable: vscode.DebugAdapterExecutable | undefined): Promise<vscode.DebugAdapterDescriptor | undefined> {
        // Create inline adapter
        const socketSession = new SocketDebugSession(session.configuration.address, session.configuration.pathMapping);
        return new vscode.DebugAdapterInlineImplementation(socketSession);
    }
}