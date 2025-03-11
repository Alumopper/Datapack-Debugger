/*---------------------------------------------------------
 * Copyright (C) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------*/
/*
 * extension.ts (and activateMockDebug.ts) forms the "plugin" that plugs into VS Code and contains the code that
 * connects VS Code with the debug adapter.
 * 
 * extension.ts contains code for launching the debug adapter in three different ways:
 * - as an external program communicating with VS Code via stdin/stdout,
 * - as a server process communicating with VS Code via sockets or named pipes, or
 * - as inlined code running in the extension itself (default).
 * 
 * Since the code in extension.ts uses node.js APIs it cannot run in the browser.
 */

'use strict';

import * as vscode from 'vscode';
import { SocketDescriptorFactory } from './SocketDescriptorFactory';
import { DebugTracker } from './debug-tracker';
import { DebugLogger } from './debug-logger';

export const activate = async (context: vscode.ExtensionContext): Promise<void> => {
    const socketDebugFactory = new SocketDescriptorFactory();
    const debugTracker = new DebugTracker([ 'warn' ]);
    new DebugLogger(debugTracker);
	console.log('activate');
    await socketDebugFactory.activate(context);
    await debugTracker.activate(context);
};