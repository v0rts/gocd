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

import {NantTask, NantTaskAttributes, Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {NantTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/nant";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Nant Task Modal", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render nant task modal", () => {
    mount();
    expect(helper.byTestId("nant-task-modal")).toBeInDOM();
  });

  it("should render build file input", () => {
    mount();

    const buildFileHelpText = "Relative path to a NAnt build file. If not specified, the path defaults to 'default.build'.";

    expect(helper.byTestId("form-field-label-build-file")).toContainText("Build File");
    expect(helper.byTestId("form-field-input-build-file")).toBeInDOM();
    expect(helper.byTestId("form-field-input-build-file")).toBeInDOM();
    expect(helper.qa("span")[0]).toContainText(buildFileHelpText);
  });

  it("should bind build file input to model", () => {
    const nantTask = new NantTask("build.xml", "target", "nant-path-exe", "/tmp", []);
    mount(nantTask);

    const attributes = nantTask.attributes() as NantTaskAttributes;

    expect(attributes.buildFile()).toBe("build.xml");
    expect(helper.byTestId("form-field-input-build-file")).toHaveValue("build.xml");

    helper.oninput(`[data-test-id="form-field-input-build-file"]`, "new-build.xml");

    expect(attributes.buildFile()).toBe("new-build.xml");
    expect(helper.byTestId("form-field-input-build-file")).toHaveValue("new-build.xml");
  });

  it("should render target input", () => {
    mount();

    const buildFileHelpText = "NAnt target(s) to run. If not specified, defaults to the default target of the build file.";

    expect(helper.byTestId("form-field-label-target")).toContainText("Target");
    expect(helper.byTestId("form-field-input-target")).toBeInDOM();
    expect(helper.byTestId("form-field-input-target")).toBeInDOM();
    expect(helper.qa("span")[1]).toContainText(buildFileHelpText);
  });

  it("should bind target input to model", () => {
    const nantTask = new NantTask("build.xml", "default", "nant-path-exec", "/tmp", []);
    mount(nantTask);

    const attributes = nantTask.attributes() as NantTaskAttributes;

    expect(attributes.target()).toBe("default");
    expect(helper.byTestId("form-field-input-target")).toHaveValue("default");

    helper.oninput(`[data-test-id="form-field-input-target"]`, "new-default");

    expect(attributes.target()).toBe("new-default");
    expect(helper.byTestId("form-field-input-target")).toHaveValue("new-default");
  });

  it("should render Nant Path input", () => {
    mount();

    const buildFileHelpText = "Path of the directory in which NAnt is installed. By default Go will assume that NAnt is in the system path.";

    expect(helper.byTestId("form-field-label-nant-path")).toContainText("NAnt Path");
    expect(helper.byTestId("form-field-input-nant-path")).toBeInDOM();
    expect(helper.byTestId("form-field-input-nant-path")).toBeInDOM();
    expect(helper.qa("span")[3]).toContainText(buildFileHelpText);
  });

  it("should bind nant path input to model", () => {
    const nantTask = new NantTask("build.xml", "default", "nant-path-exec", "/tmp", []);
    mount(nantTask);

    const attributes = nantTask.attributes() as NantTaskAttributes;

    expect(attributes.nantPath()).toBe("nant-path-exec");
    expect(helper.byTestId("form-field-input-nant-path")).toHaveValue("nant-path-exec");

    helper.oninput(`[data-test-id="form-field-input-nant-path"]`, "new-nant-path-exec");

    expect(attributes.nantPath()).toBe("new-nant-path-exec");
    expect(helper.byTestId("form-field-input-nant-path")).toHaveValue("new-nant-path-exec");
  });

  it("should render working directory input", () => {
    mount();

    const buildFileHelpText = "The directory from where nant is invoked.";

    expect(helper.byTestId("form-field-label-working-directory")).toContainText("Working Directory");
    expect(helper.byTestId("form-field-input-working-directory")).toBeInDOM();
    expect(helper.byTestId("form-field-input-working-directory")).toBeInDOM();
    expect(helper.qa("span")[2]).toContainText(buildFileHelpText);
  });

  it("should bind working directory input to model", () => {
    const nantTask = new NantTask("build.xml", "target", "nant-path-exec", "tmp", []);
    mount(nantTask);

    const attributes = nantTask.attributes() as NantTaskAttributes;

    expect(attributes.workingDirectory()).toBe("tmp");
    expect(helper.byTestId("form-field-input-working-directory")).toHaveValue("tmp");

    helper.oninput(`[data-test-id="form-field-input-working-directory"]`, "new-tmp");

    expect(attributes.workingDirectory()).toBe("new-tmp");
    expect(helper.byTestId("form-field-input-working-directory")).toHaveValue("new-tmp");
  });

  it("should render run if condition", () => {
    mount();
    expect(helper.byTestId("run-if-condition")).toBeInDOM();
  });

  it("should render run on cancel", () => {
    mount();
    expect(helper.byTestId("on-cancel-view")).toBeInDOM();
  });

  it("should not render run if condition for on cancel task", () => {
    mount(undefined, false);

    expect(helper.byTestId("nant-on-cancel-view")).not.toBeInDOM();
    expect(helper.byTestId("run-if-condition")).toBeFalsy();
  });

  it("should not render on cancel for on cancel task", () => {
    mount(undefined, false);

    expect(helper.byTestId("nant-on-cancel-view")).not.toBeInDOM();
    expect(helper.byTestId("on-cancel-view")).toBeFalsy();
  });

  describe("Read Only", () => {
    beforeEach(() => {
      mount(undefined, false, true);
    });

    it("should render readonly build file", () => {
      expect(helper.byTestId("form-field-input-build-file")).toBeDisabled();
    });

    it("should render readonly target", () => {
      expect(helper.byTestId("form-field-input-target")).toBeDisabled();
    });

    it("should render readonly working directory", () => {
      expect(helper.byTestId("form-field-input-working-directory")).toBeDisabled();
    });

    it("should render readonly nant path", () => {
      expect(helper.byTestId("form-field-input-nant-path")).toBeDisabled();
    });
  });

  function mount(task?: Task | undefined, shouldShowOnCancel: boolean = true, readonly: boolean = false) {
    helper.mount(() => {
      return new NantTaskModal(task, shouldShowOnCancel, jasmine.createSpy(), new PluginInfos(), readonly).body();
    });
  }
});
