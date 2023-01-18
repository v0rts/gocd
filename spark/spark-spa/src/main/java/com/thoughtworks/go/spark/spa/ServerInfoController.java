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
package com.thoughtworks.go.spark.spa;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class ServerInfoController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private ArtifactsDirHolder artifactsDirHolder;
    private PipelineConfigService pipelineConfigService;

    public ServerInfoController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine, ArtifactsDirHolder artifactsDirHolder, PipelineConfigService pipelineConfigService) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.artifactsDirHolder = artifactsDirHolder;
        this.pipelineConfigService = pipelineConfigService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.ServerInfo.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", authenticationHelper::checkUserAnd403);
            get("", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("go_server_version", CurrentGoCDVersion.getInstance().formatted());
        meta.put("jvm_version", System.getProperty("java.version"));
        meta.put("os_information", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        meta.put("usable_space_in_artifacts_repository", artifactsDirHolder.getArtifactsDir().getUsableSpace());
        meta.put("pipeline_count", pipelineConfigService.totalPipelinesCount());

        Map<String, Object> object = Map.of(
            "viewTitle", "Server Details",
            "meta", meta
        );

        return new ModelAndView(object, null);
    }
}
