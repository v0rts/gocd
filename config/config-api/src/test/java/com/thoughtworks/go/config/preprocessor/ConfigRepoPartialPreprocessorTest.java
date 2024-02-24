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
package com.thoughtworks.go.config.preprocessor;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConfigRepoPartialPreprocessorTest {

    private final ConfigReposConfig reposConfig;
    private final ConfigRepoConfig configRepoConfig;

    public ConfigRepoPartialPreprocessorTest() {
        reposConfig = new ConfigReposConfig();
        configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(git("http://git"), "myplug", "id");
        reposConfig.add(configRepoConfig);
    }

    @Test
    public void shouldMergePartialsSetOnConfig() {
        final PartialConfig partialConfig = PartialConfigMother.withPipeline("partial");
        partialConfig.setOrigin(new RepoConfigOrigin(configRepoConfig, "sha-1"));
        ConfigRepoPartialPreprocessor preprocessor = new ConfigRepoPartialPreprocessor();
        PipelineConfig pipelineInMain = PipelineConfigMother.createPipelineConfig("main_pipeline", "stage", "job");
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(pipelineInMain));
        cruiseConfig.setConfigRepos(reposConfig);
        cruiseConfig.setPartials(List.of(partialConfig));
        preprocessor.process(cruiseConfig);
        assertThat(cruiseConfig.getAllPipelineNames().contains(pipelineInMain.name()), is(true));
        assertThat(cruiseConfig.getAllPipelineNames().contains(partialConfig.getGroups().first().get(0).name()), is(true));
    }
}
