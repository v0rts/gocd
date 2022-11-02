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
package com.thoughtworks.go.spark.spa;

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static spark.Spark.*;

public class CompareController implements SparkController {
    private final SPAAuthenticationHelper authenticationHelper;
    private final TemplateEngine engine;
    private final PipelineService pipelineService;

    public CompareController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine, PipelineService pipelineService) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
        this.pipelineService = pipelineService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Compare.SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(Routes.Compare.COMPARE, authenticationHelper::checkPipelineViewPermissionsAnd403);
            get(Routes.Compare.COMPARE, this::index, engine);
        });
    }

    public ModelAndView index(Request request, Response response) {
        String pipelineName = request.params("pipeline_name");
        long fromCounter = getCounter(request, "from_counter");
        long toCounter = getCounter(request, "to_counter");
        if (fromCounter <= 0) {
            response.redirect(Routes.Compare.compare(pipelineName, "1", toCounter + ""));
            return null;
        }
        if (toCounter <= 0) {
            response.redirect(Routes.Compare.compare(pipelineName, fromCounter + "", "1"));
            return null;
        }
        bombIfNotFound(pipelineName, fromCounter);
        bombIfNotFound(pipelineName, toCounter);
        Map<Object, Object> object = new HashMap<>() {{
            put("viewTitle", "Compare");
            put("meta", meta(request));
        }};
        return new ModelAndView(object, null);
    }

    private void bombIfNotFound(String pipelineName, long pipelineCounter) {
        Pipeline fromPipelineInstance = pipelineService.findPipelineByNameAndCounter(pipelineName, (int) pipelineCounter);
        if (fromPipelineInstance == null) {
            throw new RecordNotFoundException(format("Pipeline [%s/%s] not found.", pipelineName, pipelineCounter));
        }
    }

    private long getCounter(Request request, String key) {
        long counter;
        try {
            counter = Long.parseLong(request.params(key));
        } catch (NumberFormatException nfe) {
            counter = 0;
        }
        return counter;
    }

    private HashMap<String, String> meta(Request request) {
        HashMap<String, String> meta = new HashMap<>();
        meta.put("pipelineName", request.params("pipeline_name"));
        meta.put("fromCounter", request.params("from_counter"));
        meta.put("toCounter", request.params("to_counter"));
        return meta;
    }
}
