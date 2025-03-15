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