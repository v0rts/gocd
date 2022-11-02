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

import {ApiResult, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {PipelineHistory, PipelineInstance, PipelineInstances} from "../pipeline_instance";
import {PipelineInstanceCRUD} from "../pipeline_instance_crud";
import {PipelineInstanceData} from "./test_data";

describe('PipelineInstanceCRUDSpec', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it('should return pipeline instance as result', (done) => {
    const apiPath = SparkRoutes.getPipelineInstance("up42", 2);
    jasmine.Ajax.stubRequest(apiPath).andReturn(pipelineInstanceResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const object       = (responseJSON.body as PipelineInstance);

      expect(object.name()).toEqual("up42");
      expect(object.counter()).toEqual(2);
      done();
    });

    PipelineInstanceCRUD.get("up42", 2).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(apiPath);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it('should return list of pipeline instances as result', (done) => {
    const apiPath = SparkRoutes.getPipelineHistory("up42");
    jasmine.Ajax.stubRequest(apiPath).andReturn(pipelineHistoryResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const object       = (responseJSON.body as PipelineHistory);

      expect(object.nextLink).toEqual("next-link");
      expect(object.previousLink).toBeUndefined();
      expect(object.pipelineName).toEqual("up42");
      expect(object.pipelineInstances[0].name()).toEqual("up42");
      expect(object.pipelineInstances[0].counter()).toEqual(2);
      done();
    });

    PipelineInstanceCRUD.history("up42").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(apiPath);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it('should return list of matching pipeline instances as result', (done) => {
    const apiPath = SparkRoutes.getMatchingPipelineInstances("up42", "2");
    jasmine.Ajax.stubRequest(apiPath).andReturn(pipelineInstancesResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const object       = (responseJSON.body as PipelineInstances);

      expect(object[0].name()).toEqual("up42");
      expect(object[0].counter()).toEqual(2);
      done();
    });

    PipelineInstanceCRUD.matchingInstances("up42", "2").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(apiPath);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });
});

function pipelineInstanceResponse() {
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag":         "some-etag"
    },
    responseText:    JSON.stringify(PipelineInstanceData.pipeline())
  };
}

function pipelineHistoryResponse() {
  const json = {
    _links:    {
      next: {
        href: "next-link"
      }
    },
    pipelines: [PipelineInstanceData.pipeline()]
  };
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag":         "some-etag"
    },
    responseText:    JSON.stringify(json)
  };
}

function pipelineInstancesResponse() {
  const json = {
    pipelines: [PipelineInstanceData.pipeline()]
  };
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag":         "some-etag"
    },
    responseText:    JSON.stringify(json)
  };
}
