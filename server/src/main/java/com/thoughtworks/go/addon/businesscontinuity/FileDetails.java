/*
 * Copyright 2022 ThoughtWorks, Inc.
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

package com.thoughtworks.go.addon.businesscontinuity;

public class FileDetails {
    private String md5;

    public FileDetails(String md5) {
        this.md5 = md5;
    }

    public String getMd5() {
        return md5;
    }

    @Override
    public String toString() {
        return "md5='" + md5+"'";
    }
}
