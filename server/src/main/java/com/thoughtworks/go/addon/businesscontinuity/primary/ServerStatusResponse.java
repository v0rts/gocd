/*
 * Copyright 2022 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.addon.businesscontinuity.primary;

import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.addon.businesscontinuity.FileDetails;

import java.util.Map;

public class ServerStatusResponse {
    private long configFilesUpdateInterval;
    private long lastConfigFilesUpdateTime;
    Map<ConfigFileType, FileDetails> fileDetailsMap;

    public ServerStatusResponse(long configFilesUpdateInterval, long lastConfigFilesUpdateTime, Map<ConfigFileType, FileDetails> fileDetailsMap) {
        this.configFilesUpdateInterval = configFilesUpdateInterval;
        this.lastConfigFilesUpdateTime = lastConfigFilesUpdateTime;
        this.fileDetailsMap = fileDetailsMap;
    }

    public long getConfigFilesUpdateInterval() {
        return configFilesUpdateInterval;
    }

    public long getLastConfigFilesUpdateTime() {
        return lastConfigFilesUpdateTime;
    }

    public Map<ConfigFileType, FileDetails> getFileDetailsMap() {
        return fileDetailsMap;
    }
}
