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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {RadioField} from "views/components/forms/input_fields";
import {EntityReOrderHandler} from "views/pages/clicky_pipeline_config/tabs/common/re_order_entity_widget";
import styles from "../stages.scss";

interface ConfigurationTypeAttrs {
  property: (value?: string) => string;
  isPipelineDefinedOriginallyFromTemplate: Stream<boolean>;
  entityReOrderHandler: EntityReOrderHandler;
  readonly: boolean;
}

export class ConfigurationTypeWidget extends MithrilComponent<ConfigurationTypeAttrs> {
  view(vnode: m.Vnode<ConfigurationTypeAttrs>) {
    const readonly = vnode.attrs.entityReOrderHandler.hasOrderChanged() || vnode.attrs.readonly || vnode.attrs.isPipelineDefinedOriginallyFromTemplate();

    return <div class={styles.configurationTypeContainer} data-test-id="configuration-type">
      <RadioField label="Configuration Type"
                  property={vnode.attrs.property}
                  readonly={readonly}
                  inline={true}
                  possibleValues={[
                    {label: "Use Template", value: "template"},
                    {label: "Define Stages", value: "stage"}
                  ]}>
      </RadioField>
    </div>;
  }
}
