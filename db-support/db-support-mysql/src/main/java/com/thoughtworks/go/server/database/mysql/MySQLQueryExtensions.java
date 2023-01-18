/*
 * Copyright 2023 Thoughtworks, Inc.
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

package com.thoughtworks.go.server.database.mysql;

import com.thoughtworks.go.server.database.QueryExtensions;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class MySQLQueryExtensions extends QueryExtensions {
    @Override
    public boolean accepts(String url) {
        return isNotBlank(url) && url.startsWith("jdbc:mysql:");
    }
}
