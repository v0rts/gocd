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
package com.thoughtworks.go.apiv4.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.Collections;

import static com.thoughtworks.go.spark.Routes.ConfigRepos.*;

public class ConfigRepoConfigRepresenterV4 {
    public static void toJSON(OutputWriter outputWriter, ConfigRepoConfig repo) {
        attachLinks(outputWriter, repo);
        outputWriter.add("id", repo.getId());
        outputWriter.add("plugin_id", repo.getPluginId());
        outputWriter.addChild("material", w -> MaterialsRepresenter.toJSON(w, repo.getRepo()));
        if (!repo.errors().isEmpty()) {
            outputWriter.addChild("errors", errorWriter -> new ErrorGetter(Collections.emptyMap()).toJSON(errorWriter, repo));
        }
        attachConfigurations(outputWriter, repo);
        outputWriter.addChildList("rules", rulesWriter -> RulesRepresenter.toJSON(rulesWriter, repo.getRules()));
    }

    public static ConfigRepoConfig fromJSON(JsonReader jsonReader) {
        MaterialConfig material = MaterialsRepresenter.fromJSON(jsonReader.readJsonObject("material"));
        ConfigRepoConfig repo = new ConfigRepoConfig();
        jsonReader.readStringIfPresent("id", repo::setId);
        jsonReader.readStringIfPresent("plugin_id", repo::setPluginId);
        repo.setRepo(material);

        repo.addConfigurations(ConfigurationPropertyRepresenter.fromJSONArrayHandlingEncryption(jsonReader, "configuration"));
        jsonReader.readArrayIfPresent("rules", array -> {
            repo.setRules(RulesRepresenter.fromJSON(array));
        });
        return repo;
    }

    private static void attachLinks(OutputWriter json, ConfigRepoConfig repo) {
        json.addLinks(links -> {
            links.addLink("self", id(repo.getId()));
            links.addAbsoluteLink("doc", DOC);
            links.addLink("find", find());
        });
    }

    private static void attachConfigurations(OutputWriter json, ConfigRepoConfig repo) {
        json.addChildList("configuration", w -> ConfigurationPropertyRepresenter.toJSON(w, repo.getConfiguration()));
    }
}
