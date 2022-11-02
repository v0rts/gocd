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
package com.thoughtworks.go.plugin.access.analytics;

import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsData;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.plugin.access.analytics.AnalyticsPluginConstants.*;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ANALYTICS_EXTENSION;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";

    @Mock(strictness = Mock.Strictness.LENIENT)
    private PluginManager pluginManager;
    @Mock
    ExtensionsRegistry extensionsRegistry;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;
    private AnalyticsExtension analyticsExtension;

    private AnalyticsMetadataStore metadataStore;

    @BeforeEach
    public void setUp() throws Exception {
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, ANALYTICS_EXTENSION, Arrays.asList("1.0", "2.0"))).thenReturn("1.0", "2.0");
        when(pluginManager.isPluginOfType(ANALYTICS_EXTENSION, PLUGIN_ID)).thenReturn(true);

        analyticsExtension = new AnalyticsExtension(pluginManager, extensionsRegistry);
        metadataStore = AnalyticsMetadataStore.instance();

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
    }

    @AfterEach
    public void tearDown() {
        metadataStore.clear();
    }

    @Test
    public void shouldTalkToPlugin_To_GetCapabilities() throws Exception {
        String responseBody = "{\n" +
                "\"supported_analytics\": [\n" +
                "  {\"type\": \"dashboard\", \"id\": \"abc\",  \"title\": \"Title 1\"},\n" +
                "  {\"type\": \"pipeline\", \"id\": \"abc\",  \"title\": \"Title 1\"}\n" +
                "]}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ANALYTICS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        com.thoughtworks.go.plugin.domain.analytics.Capabilities capabilities = analyticsExtension.getCapabilities(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), PluginConstants.ANALYTICS_EXTENSION, "1.0", REQUEST_GET_CAPABILITIES, null);

        assertThat(capabilities.supportedDashboardAnalytics(), containsInAnyOrder(new SupportedAnalytics("dashboard", "abc", "Title 1")));
        assertThat(capabilities.supportedPipelineAnalytics(), containsInAnyOrder(new SupportedAnalytics("pipeline", "abc", "Title 1")));
    }

    @Test
    public void shouldGetAnalytics() throws Exception {
        String responseBody = "{ \"view_path\": \"path/to/view\", \"data\": \"{}\" }";

        AnalyticsPluginInfo pluginInfo = new AnalyticsPluginInfo(GoPluginDescriptor.builder().id(PLUGIN_ID).build(), null, null, null);
        pluginInfo.setStaticAssetsPath("/assets/root");
        metadataStore.setPluginInfo(pluginInfo);

        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ANALYTICS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        AnalyticsData pipelineAnalytics = analyticsExtension.getAnalytics(PLUGIN_ID, "pipeline", "pipeline_with_highest_wait_time",
                Collections.singletonMap("pipeline_name", "test_pipeline"));

        String expectedRequestBody = "{" +
                "\"type\": \"pipeline\"," +
                "\"id\": \"pipeline_with_highest_wait_time\"," +
                " \"params\": {\"pipeline_name\": \"test_pipeline\"}}";

        assertRequest(requestArgumentCaptor.getValue(), PluginConstants.ANALYTICS_EXTENSION, "1.0", REQUEST_GET_ANALYTICS, expectedRequestBody);

        assertThat(pipelineAnalytics.getData(), is("{}"));
        assertThat(pipelineAnalytics.getViewPath(), is("path/to/view"));
        assertThat(pipelineAnalytics.getFullViewPath(), is("/assets/root/path/to/view"));
    }

    @Test
    public void shouldFetchStaticAssets() throws Exception {
        String responseBody = "{ \"assets\": \"assets payload\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ANALYTICS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));
        String assets = analyticsExtension.getStaticAssets(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), ANALYTICS_EXTENSION, "1.0", REQUEST_GET_STATIC_ASSETS, null);

        assertThat(assets, is("assets payload"));
    }

    @Test
    public void shouldErrorOutInAbsenceOfStaticAssets() {
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ANALYTICS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, "{}"));

        assertThatThrownBy(() -> analyticsExtension.getStaticAssets(PLUGIN_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No assets defined!");
    }

    @Test
    public void shouldVerifyGoSupportedVersion() {
        assertTrue(analyticsExtension.goSupportedVersions().contains("1.0"));
        assertTrue(analyticsExtension.goSupportedVersions().contains("2.0"));
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThatJson(requestBody).isEqualTo(goPluginApiRequest.requestBody());
    }
}
