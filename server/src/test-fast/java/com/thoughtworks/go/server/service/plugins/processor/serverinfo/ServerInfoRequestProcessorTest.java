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
package com.thoughtworks.go.server.service.plugins.processor.serverinfo;

import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.thoughtworks.go.server.service.plugins.processor.serverinfo.ServerInfoRequestProcessor.GET_SERVER_INFO;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ServerInfoRequestProcessorTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private GoPluginExtension pluginExtension;
    @Mock
    private GoPluginDescriptor pluginDescriptor;
    private PluginRequestProcessorRegistry processorRegistry;
    private ServerInfoRequestProcessor processor;
    private ServerConfig serverConfig;
    private String pluginId = "plugin_id";

    @BeforeEach
    public void setUp() throws Exception {
        serverConfig = new ServerConfig();
        serverConfig.ensureServerIdExists();
        serverConfig.setSecureSiteUrl("https://example.com/go");
        serverConfig.setSiteUrl("http://example.com:8153/go");

        processorRegistry = new PluginRequestProcessorRegistry();
        processor = new ServerInfoRequestProcessor(processorRegistry, goConfigService);

        lenient().when(goConfigService.serverConfig()).thenReturn(serverConfig);
        lenient().when(pluginExtension.extensionName()).thenReturn("extension1");
        lenient().when(pluginDescriptor.id()).thenReturn(pluginId);
    }

    @Test
    public void shouldRegisterAPIRequestWithProcessor() {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_INFO, "1.0", new GoPluginIdentifier("extension1", List.of("1.0")));
        assertThat(processorRegistry.canProcess(request), is(true));
    }

    @Test
    public void shouldReturnAServerIdInJSONForm() {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_INFO, "1.0", new GoPluginIdentifier("extension1", List.of("1.0")));


        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(),
                is(format("{\"server_id\":\"%s\",\"site_url\":\"%s\",\"secure_site_url\":\"%s\"}",
                        serverConfig.getServerId(), serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl())));
    }

    @Test
    public void shouldReturnSuccessForServerInfoV2() {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_INFO, "2.0", new GoPluginIdentifier("extension1", List.of("1.0")));

        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(200));
    }

    @Test
    public void shouldReturnAErrorResponseIfExtensionDoesNotSupportServerInfo() {
        DefaultGoApiRequest request = new DefaultGoApiRequest(GET_SERVER_INFO, "bad-version", new GoPluginIdentifier("foo", List.of("1.0")));

        GoApiResponse response = processor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(400));
    }
}
