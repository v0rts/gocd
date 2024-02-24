/*
 * Copyright 2024 Thoughtworks, Inc.
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

import {JobState} from "models/shared/job_state";
import {JobStateTransitionJSON} from "models/shared/job_state_transition";

export enum Result {
  Passed, Failed, Cancelled, Unknown
}

export enum StageState {
  Building, Failing, Passed, Failed, Unknown, Cancelled
}

export interface JobJSON {
  name: string;
  state: JobState;
  result: Result | string;
  scheduled_date: number;
  rerun: boolean;
  original_job_id: number | null;
  agent_uuid: string | null;
  job_state_transitions: JobStateTransitionJSON[];
}

export interface StageInstanceJSON {
  name: string;
  counter: number;
  approval_type: string;
  approved_by: string;
  scheduled_at: number;
  last_transitioned_time: number;
  cancelled_by?: string;
  result: Result | string;
  rerun_of_counter: number | null;
  fetch_materials: boolean;
  clean_working_directory: boolean;
  artifacts_deleted: boolean;
  pipeline_name: string;
  pipeline_counter: number;
  jobs: JobJSON[];
}
