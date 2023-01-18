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

package com.thoughtworks.go.apiv2.apiinfo.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.spark.DeprecatedAPI;
import com.thoughtworks.go.spark.spring.RouteEntry;
import spark.RouteImpl;
import spark.utils.SparkUtils;

import java.util.List;
import java.util.stream.Collectors;

public class RouteEntryRepresenter {
    public static void toJSON(OutputListWriter writer, List<RouteEntry> routes) {
        routes.forEach(entry -> writer.addChild(entryWriter -> {
            entryWriter
                    .add("method", entry.getHttpMethod().name())
                    .add("path", entry.getPath())
                    .add("version", entry.getAcceptedType())
                    .addChildList("path_params", getParams(entry));

            Class<?> routeHandlerClass = ((RouteImpl) entry.getTarget()).delegate().getClass();

            // Generally routes are lambdas nested within a controller class, so we can find the controller
            // by looking for the nest host of the route
            Class<?> controllerClass = routeHandlerClass.getNestHost();
            DeprecatedAPI deprecatedAPI = controllerClass.getAnnotation(DeprecatedAPI.class);
            addDeprecatedApiInfo(entryWriter, deprecatedAPI);
        }));
    }

    private static void addDeprecatedApiInfo(OutputWriter entryWriter, DeprecatedAPI deprecatedAPI) {
        if (deprecatedAPI == null) {
            addNonDeprecatedApiInfo(entryWriter);
            return;
        }

        entryWriter.addChild("deprecation_info", deprecationWriter -> {
            deprecationWriter.add("is_deprecated", true);
            deprecationWriter.add("deprecated_api_version", deprecatedAPI.deprecatedApiVersion());
            deprecationWriter.add("successor_api_version", deprecatedAPI.successorApiVersion());
            deprecationWriter.add("deprecated_in", deprecatedAPI.deprecatedIn());
            deprecationWriter.add("removal_in", deprecatedAPI.removalIn());
        });
    }

    private static void addNonDeprecatedApiInfo(OutputWriter entryWriter) {
        entryWriter.addChild("deprecation_info", deprecationWriter -> deprecationWriter.add("is_deprecated", false));
    }

    public static List<String> getParams(RouteEntry entry) {
        return SparkUtils.convertRouteToList(entry.getPath())
                .stream()
                .filter(SparkUtils::isParam)
                .collect(Collectors.toList());
    }
}
