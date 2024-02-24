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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(MockitoExtension.class)
public class UpdateTemplateAuthConfigCommandTest {
    @Mock
    private EntityHashingService entityHashingService;

    @Mock
    private ExternalArtifactsService externalArtifactsService;

    @Mock
    private SecurityService securityService;

    private PipelineTemplateConfig pipelineTemplateConfig;
    private Authorization authorization;

    @BeforeEach
    public void setup() {
        pipelineTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"));
        authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("user"))));
        pipelineTemplateConfig.setAuthorization(authorization);
    }

    @Test
    public void shouldReplaceOnlyTemplateAuthorizationWhileUpdatingTheTemplate() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));
        Authorization templateAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("foo"))));
        updatedTemplateConfig.setAuthorization(templateAuthorization);
        cruiseConfig.addTemplate(pipelineTemplateConfig);

        UpdateTemplateAuthConfigCommand command = new UpdateTemplateAuthConfigCommand(updatedTemplateConfig, templateAuthorization, new Username(new CaseInsensitiveString("user")), securityService, new HttpLocalizedOperationResult(), "md5", entityHashingService, externalArtifactsService);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.getTemplates().contains(pipelineTemplateConfig), is(true));
        assertThat(cruiseConfig.getTemplates().contains(updatedTemplateConfig), is(false));
        Authorization expectedTemplateAuthorization = cruiseConfig.getTemplateByName(pipelineTemplateConfig.name()).getAuthorization();
        assertNotEquals(expectedTemplateAuthorization, authorization);
        assertThat(expectedTemplateAuthorization, is(templateAuthorization));

    }

    @Test
    public void shouldCopyOverErrorsOnAuthorizationFromThePreprocessedTemplateConfig() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        PipelineTemplateConfig updatedTemplateConfig = new PipelineTemplateConfig(new CaseInsensitiveString("template"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage", "job"), StageConfigMother.oneBuildPlanWithResourcesAndMaterials("stage2"));
        Authorization templateAuthorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString(""))));
        updatedTemplateConfig.setAuthorization(templateAuthorization);
        cruiseConfig.addTemplate(updatedTemplateConfig);

        UpdateTemplateAuthConfigCommand command = new UpdateTemplateAuthConfigCommand(updatedTemplateConfig, templateAuthorization, new Username(new CaseInsensitiveString("user")), securityService, new HttpLocalizedOperationResult(), "md5", entityHashingService, externalArtifactsService);
        assertFalse(command.isValid(cruiseConfig));

        assertThat(templateAuthorization.getAllErrors().get(0).getAllOn("roles"), is(List.of("Role \"\" does not exist.")));
    }
}
