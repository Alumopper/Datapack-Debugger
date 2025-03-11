/*********************************************************************
 * Copyright (c) 2023 Arm Limited and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *********************************************************************/

import { DebugTracker } from './debug-tracker';
import { logger } from './logger';

export class DebugLogger {
    public constructor(tracker: DebugTracker) {
        tracker.onWillStartSession(session => logger.debug(session.configuration));
        tracker.onWillReceiveMessage(message => logger.debug(`UI >>> Debugger\n${message}`));
        tracker.onDidSendMessage(message => logger.debug(`UI <<< Debugger\n${message}`));
    }
}