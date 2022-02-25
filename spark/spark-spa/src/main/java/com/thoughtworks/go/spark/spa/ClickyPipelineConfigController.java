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

package com.thoughtworks.go.spark.spa;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.service.GoConfigService;
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

public class ClickyPipelineConfigController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private GoConfigService goConfigService;
    private final TemplateEngine engine;

    public ClickyPipelineConfigController(SPAAuthenticationHelper authenticationHelper, GoConfigService goConfigService, TemplateEngine engine) {
        this.authenticationHelper = authenticationHelper;
        this.goConfigService = goConfigService;
        this.engine = engine;
    }

    @Override
    public String controllerBasePath() {
        return Routes.PipelineConfig.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(Routes.PipelineConfig.NAME + "/edit", authenticationHelper::checkPipelineGroupAdminOfPipelineOrGroupInURLUserAnd403);
            get(Routes.PipelineConfig.NAME + "/edit", this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        String pipelineName = request.params("pipeline_name");
        Map<Object, Object> object = new HashMap<>() {{
            put("viewTitle", "Pipeline");
            put("meta", meta(pipelineName));
        }};
        return new ModelAndView(object, null);
    }

    private Map<String, Object> meta(String pipelineName) {
        final Map<String, Object> meta = new HashMap<>();
        meta.put("pipelineName", pipelineName);
        meta.put("pipelineGroupName", this.goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName)));
        return meta;
    }
}
