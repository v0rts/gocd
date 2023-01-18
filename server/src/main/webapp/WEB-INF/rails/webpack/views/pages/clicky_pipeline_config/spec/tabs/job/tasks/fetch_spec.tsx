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

import Stream from "mithril/stream";
import {FetchArtifactTask, Task} from "models/pipeline_configs/task";
import {Configurations} from "models/shared/configuration";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ArtifactPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {FetchArtifactTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Fetch Task Modal", () => {
  const helper = new TestHelper();

  let modal: FetchArtifactTaskModal;

  afterEach(helper.unmount.bind(helper));

  it("should render fetch task modal", () => {
    mount();
    expect(helper.byTestId("fetch-artifact-task-modal")).toBeInDOM();
  });

  it("should render GoCD or plugin fetch artifact task selection", () => {
    mount();

    expect(helper.byTestId("form-field-label")).toContainText("Type of Fetch Artifact");

    expect(helper.byTestId("input-field-for-gocd")).toBeInDOM();
    expect(helper.byTestId("input-field-for-external")).toBeInDOM();

    expect(helper.byTestId("radio-gocd")).toBeChecked();
    expect(helper.byTestId("radio-external")).not.toBeChecked();
  });

  it("should render GoCD fetch artifact task view", () => {
    mount();

    expect(helper.byTestId("radio-gocd")).toBeChecked();
    expect(helper.byTestId("radio-external")).not.toBeChecked();
    expect(helper.byTestId("built-in-fetch-artifact-view")).toBeInDOM();
    expect(helper.byTestId("external-artifact-view")).not.toBeInDOM();
  });

  it("should render external fetch artifact task view", () => {
    const task = new FetchArtifactTask(
      "external", undefined, "", "", false,
      undefined, undefined,
      undefined, new Configurations([]), []);

    mount(task);

    expect(helper.byTestId("radio-gocd")).not.toBeChecked();
    expect(helper.byTestId("radio-external")).toBeChecked();
    expect(helper.byTestId("built-in-fetch-artifact-view")).not.toBeInDOM();
    expect(helper.byTestId("external-fetch-artifact-view")).toBeInDOM();
  });

  describe("Read Only", () => {
    beforeEach(() => {
      mount(undefined, false, true);
    });

    it("should render disabled GoCD or plugin fetch artifact task selection", () => {
      expect(helper.byTestId("radio-gocd")).toBeDisabled();
      expect(helper.byTestId("radio-external")).toBeDisabled();
    });
  });

  function mount(task?: Task | undefined, shouldShowOnCancel: boolean = true, readonly: boolean = false) {
    helper.mount(() => {
      modal = new FetchArtifactTaskModal(task,
                                         shouldShowOnCancel,
                                         jasmine.createSpy(),
                                         new PluginInfos(PluginInfo.fromJSON(ArtifactPluginInfo.docker())),
                                         readonly,
                                         Stream({}));
      return modal.body();
    });
  }
});
