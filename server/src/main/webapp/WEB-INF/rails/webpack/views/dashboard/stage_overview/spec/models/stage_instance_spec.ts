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

import {StageInstance} from "../../models/stage_instance";
import {Result, StageInstanceJSON} from "../../models/types";
import {TestData} from "../test_data";

describe('Stage Instance', () => {

  let stageInstanceJson: StageInstanceJSON;

  beforeEach(() => {
    stageInstanceJson = TestData.stageInstanceJSON();
  });

  it('should deserialize from json', () => {
    // @ts-ignore
    const _json = StageInstance.fromJSON(stageInstanceJson);
  });

  it('should provide triggered by information', () => {
    const json = StageInstance.fromJSON(stageInstanceJson);
    expect(json.triggeredBy()).toBe('admin');
  });

  it('should provide triggered on information', () => {
    const json: StageInstance = StageInstance.fromJSON(stageInstanceJson);
    expect(json.triggeredOn()).toBe(TestData.unixTime(json.stageScheduledAtInSecs()));
  });

  describe("Stage Duration", () => {
    it("should provide stage duration as in progress when some of the jobs from the stage are still in progress", () => {
      stageInstanceJson.jobs[0].result = Result[Result.Unknown];
      stageInstanceJson.jobs[0].job_state_transitions.pop();
      const json = StageInstance.fromJSON(stageInstanceJson);
      expect(json.stageDuration()).toBe('in progress');
    });

    it("should provide stage duration", () => {
      // modify the json
      const json = StageInstance.fromJSON(stageInstanceJson);
      expect(json.stageDuration()).toBe('01h 40m 50s');
    });
  });

  it('should answer false whether the stage is not cancelled', () => {
    const json = StageInstance.fromJSON(stageInstanceJson);
    expect(json.isCancelled()).toBe(false);
  });

  it('should answer true whether the stage is cancelled', () => {
    stageInstanceJson.result = Result[Result.Cancelled];
    const json = StageInstance.fromJSON(stageInstanceJson);
    expect(json.isCancelled()).toBe(true);
  });

  it('should provide the cancelled by information', () => {
    stageInstanceJson.result = Result[Result.Cancelled];
    stageInstanceJson.cancelled_by = 'admin';
    const json = StageInstance.fromJSON(stageInstanceJson);
    expect(json.cancelledBy()).toBe('admin');
  });

  it('should answer whether the stage is in progress', () => {
    stageInstanceJson.jobs.push(TestData.failedJobWhenUploadingArtifacts());
    const json = StageInstance.fromJSON(stageInstanceJson);
    expect(json.stageDuration()).toBe('in progress');
  });
});
