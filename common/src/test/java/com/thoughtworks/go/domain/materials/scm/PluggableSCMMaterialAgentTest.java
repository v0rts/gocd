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
package com.thoughtworks.go.domain.materials.scm;

import com.google.gson.Gson;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.util.command.TaggedStreamConsumer.PREP_ERR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PluggableSCMMaterialAgentTest {
    @Mock
    private SCMExtension scmExtension;

    @Mock
    private ConsoleOutputStreamConsumer consumer;

    private ArgumentCaptor<SCMPropertyConfiguration> scmConfiguration;
    private ArgumentCaptor<SCMRevision> scmRevision;

    @BeforeEach
    void setup() {
        scmConfiguration = ArgumentCaptor.forClass(SCMPropertyConfiguration.class);
        scmRevision = ArgumentCaptor.forClass(SCMRevision.class);
    }

    @Test
    void shouldTalkToPluginCheckoutForPrepare() {
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        pluggableSCMMaterial.setFolder("destination-folder");
        Modification modification = ModificationsMother.oneModifiedFile("r1");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        modification.setAdditionalData(new Gson().toJson(additionalData));
        MaterialRevision revision = new MaterialRevision(pluggableSCMMaterial, modification);
        String pipelineFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        String destinationFolder = new File(pipelineFolder, "destination-folder").getAbsolutePath();
        PluggableSCMMaterialAgent pluggableSCMMaterialAgent = new PluggableSCMMaterialAgent(scmExtension, revision, new File(pipelineFolder), consumer);
        when(scmExtension.checkout(eq("pluginid"), scmConfiguration.capture(), eq(destinationFolder), scmRevision.capture())).thenReturn(new Result());

        pluggableSCMMaterialAgent.prepare();

        verify(scmExtension).checkout(any(String.class), any(SCMPropertyConfiguration.class), any(String.class), any(SCMRevision.class));
        assertThat(scmConfiguration.getValue().size()).isEqualTo(2);
        assertThat(scmConfiguration.getValue().get("k1").getValue()).isEqualTo("v1");
        assertThat(scmConfiguration.getValue().get("k2").getValue()).isEqualTo("v2");
        assertThat(scmRevision.getValue().getRevision()).isEqualTo("r1");
        assertThat(scmRevision.getValue().getTimestamp()).isEqualTo(modification.getModifiedTime());
        assertThat(scmRevision.getValue().getData().size()).isEqualTo(2);
        assertThat(scmRevision.getValue().getDataFor("a1")).isEqualTo("v1");
        assertThat(scmRevision.getValue().getDataFor("a2")).isEqualTo("v2");
    }

    @Test
    void shouldSendResolvedValuesToPluginCheckoutForPrepareWhenSecretParamsArePresent() {
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        pluggableSCMMaterial.getScmConfig().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][lookup_username]}}"));
        pluggableSCMMaterial.getScmConfig().getConfiguration().get(0).getSecretParams().get(0).setValue("some-dummy-value");
        pluggableSCMMaterial.setFolder("destination-folder");
        Modification modification = ModificationsMother.oneModifiedFile("r1");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        modification.setAdditionalData(new Gson().toJson(additionalData));
        MaterialRevision revision = new MaterialRevision(pluggableSCMMaterial, modification);
        String pipelineFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        String destinationFolder = new File(pipelineFolder, "destination-folder").getAbsolutePath();
        PluggableSCMMaterialAgent pluggableSCMMaterialAgent = new PluggableSCMMaterialAgent(scmExtension, revision, new File(pipelineFolder), consumer);
        when(scmExtension.checkout(eq("pluginid"), scmConfiguration.capture(), eq(destinationFolder), scmRevision.capture())).thenReturn(new Result());

        pluggableSCMMaterialAgent.prepare();

        verify(scmExtension).checkout(any(String.class), any(SCMPropertyConfiguration.class), any(String.class), any(SCMRevision.class));
        assertThat(scmConfiguration.getValue().size()).isEqualTo(2);
        assertThat(scmConfiguration.getValue().get("k1").getValue()).isEqualTo("some-dummy-value");
        assertThat(scmConfiguration.getValue().get("k2").getValue()).isEqualTo("v2");
        assertThat(scmRevision.getValue().getRevision()).isEqualTo("r1");
        assertThat(scmRevision.getValue().getTimestamp()).isEqualTo(modification.getModifiedTime());
        assertThat(scmRevision.getValue().getData().size()).isEqualTo(2);
        assertThat(scmRevision.getValue().getDataFor("a1")).isEqualTo("v1");
        assertThat(scmRevision.getValue().getDataFor("a2")).isEqualTo("v2");
    }

    @Test
    void shouldLogToStdOutWhenPluginSendsCheckoutResultWithSuccessMessages() {
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration());
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        pluggableSCMMaterial.setFolder("destination-folder");
        pluggableSCMMaterial.setSCMConfig(scmConfig);
        Modification modification = ModificationsMother.oneModifiedFile("r1");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        modification.setAdditionalData(new Gson().toJson(additionalData));
        MaterialRevision revision = new MaterialRevision(pluggableSCMMaterial, modification);
        String pipelineFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        String destinationFolder = new File(pipelineFolder, "destination-folder").getAbsolutePath();

        PluggableSCMMaterialAgent pluggableSCMMaterialAgent = new PluggableSCMMaterialAgent(scmExtension, revision, new File(pipelineFolder), consumer);
        when(scmExtension.checkout(eq("pluginid"), any(), eq(destinationFolder), any()))
                .thenReturn(new Result().withSuccessMessages("Material scm-name is updated."));

        pluggableSCMMaterialAgent.prepare();

        verify(consumer, times(1)).stdOutput("Material scm-name is updated.");
    }

    @Test
    void shouldLogToErrorOutputWhenPluginReturnResultWithCheckoutFailure() {
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration());
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        pluggableSCMMaterial.setFolder("destination-folder");
        pluggableSCMMaterial.setSCMConfig(scmConfig);
        Modification modification = ModificationsMother.oneModifiedFile("r1");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        modification.setAdditionalData(new Gson().toJson(additionalData));
        MaterialRevision revision = new MaterialRevision(pluggableSCMMaterial, modification);
        String pipelineFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        String destinationFolder = new File(pipelineFolder, "destination-folder").getAbsolutePath();

        PluggableSCMMaterialAgent pluggableSCMMaterialAgent = new PluggableSCMMaterialAgent(scmExtension, revision, new File(pipelineFolder), consumer);
        when(scmExtension.checkout(eq("pluginid"), any(), eq(destinationFolder), any()))
                .thenReturn(new Result().withErrorMessages("No such revision."));

        assertThatCode(() -> pluggableSCMMaterialAgent.prepare())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Material scm-name checkout failed: No such revision.");

        verify(consumer, times(1)).taggedErrOutput(PREP_ERR, "Material scm-name checkout failed: No such revision.");
    }

    @Test
    void shouldLogToErrorOutputWhenPluginSendsErrorResponse() {
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration());
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        pluggableSCMMaterial.setFolder("destination-folder");
        pluggableSCMMaterial.setSCMConfig(scmConfig);
        Modification modification = ModificationsMother.oneModifiedFile("r1");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        modification.setAdditionalData(new Gson().toJson(additionalData));
        MaterialRevision revision = new MaterialRevision(pluggableSCMMaterial, modification);
        String pipelineFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        String destinationFolder = new File(pipelineFolder, "destination-folder").getAbsolutePath();

        PluggableSCMMaterialAgent pluggableSCMMaterialAgent = new PluggableSCMMaterialAgent(scmExtension, revision, new File(pipelineFolder), consumer);
        when(scmExtension.checkout(eq("pluginid"), any(), eq(destinationFolder), any())).thenThrow(new RuntimeException("some message from plugin"));

        assertThatCode(() -> pluggableSCMMaterialAgent.prepare())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("some message from plugin");

        verify(consumer, times(1)).taggedErrOutput(PREP_ERR, "Material scm-name checkout failed: some message from plugin");
    }
}
