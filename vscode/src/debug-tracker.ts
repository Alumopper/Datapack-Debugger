import * as vscode from 'vscode';

export interface ExitData {
    session: vscode.DebugSession;
    code: number | undefined;
}

export class DebugTracker {
    private _onWillStartSession: vscode.EventEmitter<vscode.DebugSession> = new vscode.EventEmitter<vscode.DebugSession>();
    public readonly onWillStartSession: vscode.Event<vscode.DebugSession> = this._onWillStartSession.event;

    private _onWillStopSession: vscode.EventEmitter<vscode.DebugSession> = new vscode.EventEmitter<vscode.DebugSession>();
    public readonly onWillStopSession: vscode.Event<vscode.DebugSession> = this._onWillStopSession.event;

    private _onExit: vscode.EventEmitter<ExitData> = new vscode.EventEmitter<ExitData>();
    public readonly onExit: vscode.Event<ExitData> = this._onExit.event;

    private _onWillReceiveMessage: vscode.EventEmitter<string> = new vscode.EventEmitter<string>();
    public readonly onWillReceiveMessage: vscode.Event<string> = this._onWillReceiveMessage.event;

    private _onDidSendMessage: vscode.EventEmitter<string> = new vscode.EventEmitter<string>();
    public readonly onDidSendMessage: vscode.Event<string> = this._onDidSendMessage.event;

    public constructor(protected debugTypes: string[]) {
    }

    public async activate(context: vscode.ExtensionContext): Promise<void> {
        // Use vscode debug tracker
        const createDebugAdapterTracker = (session: vscode.DebugSession): vscode.DebugAdapterTracker => ({
            onWillStartSession: () => this._onWillStartSession.fire(session),
            onWillStopSession: () => this._onWillStopSession.fire(session),
            onExit: (code: number | undefined, _signal: string | undefined) => this._onExit.fire({ session, code }),
            onWillReceiveMessage: message => this._onWillReceiveMessage.fire(JSON.stringify(message)),
            onDidSendMessage: message => this._onDidSendMessage.fire(JSON.stringify(message))
        });

        const trackers = this.debugTypes.map(debugType => vscode.debug.registerDebugAdapterTrackerFactory(debugType, { createDebugAdapterTracker }));
        context.subscriptions.push(...trackers);
    }
}