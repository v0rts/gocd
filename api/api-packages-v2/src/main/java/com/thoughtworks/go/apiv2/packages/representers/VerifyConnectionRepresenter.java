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

package com.thoughtworks.go.apiv2.packages.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;

public class VerifyConnectionRepresenter {
    public static void toJSON(OutputWriter outputWriter, HttpLocalizedOperationResult result, PackageDefinition packageDefinition) {
        outputWriter.add("message", result.message())
                .addChild("package", writer -> PackageDefinitionRepresenter.toJSON(writer, packageDefinition));
    }
}
