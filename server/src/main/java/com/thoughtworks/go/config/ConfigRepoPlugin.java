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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.configrepo.ConfigFileList;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.access.configrepo.ExportedConfig;
import com.thoughtworks.go.plugin.access.configrepo.InvalidPartialConfigException;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.configrepo.contract.CRPipeline;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigRepoPlugin implements PartialConfigProvider {
    private final ConfigConverter configConverter;
    private final ConfigRepoExtension crExtension;
    private final String pluginId;

    public ConfigRepoPlugin(ConfigConverter configConverter, ConfigRepoExtension crExtension, String pluginId) {
        this.configConverter = configConverter;
        this.crExtension = crExtension;
        this.pluginId = pluginId;
    }

    /**
     * Provides decrypted configuration properties to the config-repo plugin request.
     *
     * @param configuration the raw properties
     * @return a list of decrypted, serializable configuration properties
     */
    public static List<CRConfigurationProperty> getCrConfigurations(Configuration configuration) {
        return configuration.stream().
                map((prop) -> new CRConfigurationProperty(
                        prop.getConfigKeyName(),
                        prop.getValue(), // decrypt any secrets
                        null
                )).
                collect(Collectors.toList());
    }

    @Override
    public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
        Collection<CRConfigurationProperty> cRconfigurations = getCrConfigurations(context.configuration());
        CRParseResult crPartialConfig = parseDirectory(configRepoCheckoutDirectory, cRconfigurations);
        return configConverter.toPartialConfig(crPartialConfig, context);
    }

    public String id() {
        return this.pluginId;
    }

    @Override
    public String displayName() {
        return "Plugin " + this.pluginId;
    }

    public ExportedConfig pipelineExport(PipelineConfig pipelineConfig, String groupName) {
        CRPipeline crPipeline = configConverter.pipelineConfigToCRPipeline(pipelineConfig, groupName);
        return this.crExtension.pipelineExport(this.pluginId, crPipeline);
    }

    public ConfigFileList getConfigFiles(File configRepoCheckoutDirectory, Collection<CRConfigurationProperty> crConfigurationProperties) {
        return this.crExtension.getConfigFiles(this.pluginId, configRepoCheckoutDirectory.getAbsolutePath(), crConfigurationProperties);
    }

    public PartialConfig parseContent(Map<String, String> content, PartialConfigLoadContext context) {
        CRParseResult parseResult = this.crExtension.parseContent(pluginId, content);
        if (parseResult.hasErrors()) {
            throw new InvalidPartialConfigException(parseResult, parseResult.getErrors().yamlFormat());
        }
        return configConverter.toPartialConfig(parseResult, context);
    }

    public CRParseResult parseDirectory(File configRepoCheckoutDirectory, Collection<CRConfigurationProperty> cRconfigurations) {
        CRParseResult crParseResult = this.crExtension.parseDirectory(this.pluginId, configRepoCheckoutDirectory.getAbsolutePath(), cRconfigurations);
        if (crParseResult.hasErrors())
            throw new InvalidPartialConfigException(crParseResult, crParseResult.getErrors().getErrorsAsText());
        return crParseResult;
    }
}
