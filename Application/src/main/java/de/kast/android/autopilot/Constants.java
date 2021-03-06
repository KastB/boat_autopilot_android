/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kast.android.autopilot;

import de.kast.android.autopilot.service.AutopilotService;

/**
 * Defines several constants used between {@link AutopilotService} and the UI.
 */
public interface Constants {

    // Message types sent from the AutopilotService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ_RAW = 2;
    public static final int MESSAGE_READ_PROCESSED = 3;
    public static final int MESSAGE_READ_HISTORY = 4;
    public static final int MESSAGE_WRITE = 5;
    public static final int MESSAGE_DEVICE_NAME = 6;
    public static final int MESSAGE_TOAST = 7;
}
