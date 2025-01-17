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
package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicPipelineConfigsTest {
    private static final String PIPELINES_WITH_PERMISSION = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server>
              <artifacts>
                 <artifactsDir>other-artifacts</artifactsDir>
              </artifacts>
                <security>
                  <roles>
                    <role name="admin" />
                    <role name="mingle" />
                  </roles>
                </security>
              </server>
              <pipelines group="defaultGroup">
             <authorization>
             %%s   </authorization>
                <pipeline name="pipeline1" labeltemplate="alpha.${COUNT}">
                   <materials>
                     <svn url="foobar" checkexternals="true" />
                   </materials>
                  <stage name="mingle">
                   <jobs>
                     <job name="functional">
                       <tasks><ant /></tasks>
                       <artifacts>
                         <artifact type="build" src="artifact1.xml" dest="cruise-output" />
                       </artifacts>
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(GoConstants.CONFIG_SCHEMA_VERSION);

    private static final String VIEW_PERMISSION = """
                <view>
                  <user>jez</user>
                  <user>lqiao</user>
                  <role>mingle</role>
                </view>
            """;

    private static final String OPERATION_PERMISSION = """
                <operate>
                  <user>jez</user>
                  <user>lqiao</user>
                  <role>mingle</role>
                </operate>
            """;

    @Test
    public void shouldWriteOperatePermissionForGroupCorrectly() {
        OperationConfig operationConfig = new OperationConfig(new AdminUser(new CaseInsensitiveString("jez")), new AdminUser(new CaseInsensitiveString("lqiao")), new AdminRole(
                new CaseInsensitiveString("mingle")));
        Authorization authorization = new Authorization(operationConfig);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs(authorization, pipelineConfig);
        MagicalGoConfigXmlWriter xmlWriter = new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()
        );
        String xml = xmlWriter.toXmlPartial(pipelineConfigs);
        assertThat(xml).isEqualTo("""
                <pipelines>
                  <authorization>
                    <operate>
                      <user>jez</user>
                      <user>lqiao</user>
                      <role>mingle</role>
                    </operate>
                  </authorization>
                  <pipeline name="pipeline1">
                    <materials>
                      <svn url="http://some/svn/url" dest="svnDir" materialName="http___some_svn_url" />
                    </materials>
                    <stage name="mingle">
                      <jobs />
                    </stage>
                  </pipeline>
                </pipelines>""");

    }

    @Test
    public void shouldLoadOperationPermissionForPipelines() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(configureAuthorization(OPERATION_PERMISSION));
        cruiseConfig.initializeServer();
        PipelineConfigs group = cruiseConfig.getGroups().first();

        assertThat(group.getAuthorization()).isInstanceOf(Authorization.class);

        AdminsConfig actual = group.getAuthorization().getOperationConfig();

        assertion(actual);
    }

    @Test
    public void shouldLoadOperationAndViewPermissionForPipelinesNoMatterTheConfigOrder() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(configureAuthorization(OPERATION_PERMISSION + VIEW_PERMISSION));
        cruiseConfig.initializeServer();
        PipelineConfigs group = cruiseConfig.getGroups().first();

        assertThat(group.getAuthorization()).isInstanceOf(Authorization.class);

        AdminsConfig actualView = group.getAuthorization().getViewConfig();
        AdminsConfig actualOperation = group.getAuthorization().getOperationConfig();

        assertion(actualView);
        assertion(actualOperation);
    }

    @Test
    public void shouldLoadViewAndOperationPermissionForPipelinesNoMatterTheConfigOrder() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(configureAuthorization(VIEW_PERMISSION + OPERATION_PERMISSION));
        cruiseConfig.initializeServer();
        PipelineConfigs group = cruiseConfig.getGroups().first();

        assertThat(group.getAuthorization()).isInstanceOf(Authorization.class);

        AdminsConfig actualView = group.getAuthorization().getViewConfig();
        AdminsConfig actualOperation = group.getAuthorization().getOperationConfig();

        assertion(actualView);
        assertion(actualOperation);
    }

    private void assertion(AdminsConfig actualView) {
        assertThat(actualView).contains(new AdminUser(new CaseInsensitiveString("jez")));
        assertThat(actualView).contains(new AdminUser(new CaseInsensitiveString("lqiao")));
        assertThat(actualView).contains(new AdminRole(new CaseInsensitiveString("mingle")));
    }

    private String configureAuthorization(String permission) {
        return String.format(PIPELINES_WITH_PERMISSION, permission);
    }
}
