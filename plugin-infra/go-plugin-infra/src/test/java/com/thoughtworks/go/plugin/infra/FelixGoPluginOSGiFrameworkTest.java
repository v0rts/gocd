/*
 * Copyright 2023 Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginBundleDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginLoggingService;
import com.thoughtworks.go.plugin.infra.service.DefaultPluginRegistryService;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.plugin.internal.api.PluginRegistryService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.felix.framework.util.FelixConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

;

@ExtendWith(MockitoExtension.class)
class FelixGoPluginOSGiFrameworkTest {
    private static final String TEST_SYMBOLIC_NAME = "testplugin.descriptorValidator";

    @Mock
    private BundleContext bundleContext;
    @Mock
    private Bundle bundle;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private Framework framework;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private PluginRegistry registry;
    @Mock
    private SystemEnvironment systemEnvironment;
    private FelixGoPluginOSGiFramework spy;

    @BeforeEach
    void setUp() {
        FelixGoPluginOSGiFramework goPluginOSGiFramework = new FelixGoPluginOSGiFramework(registry, systemEnvironment) {
            @Override
            protected Map<String, String> generateOSGiFrameworkConfig() {
                Map<String, String> config = super.generateOSGiFrameworkConfig();
                config.put(FelixConstants.RESOLVER_PARALLELISM, "1");
                return config;
            }
        };

        spy = spy(goPluginOSGiFramework);
        when(framework.getBundleContext()).thenReturn(bundleContext);
        when(registry.getPlugin(TEST_SYMBOLIC_NAME)).thenReturn(buildExpectedDescriptor(TEST_SYMBOLIC_NAME));
        lenient().doReturn(framework).when(spy).getFelixFramework(any());
    }

    @AfterEach
    void tearDown() {
        spy.stop();
    }

    @Test
    void shouldRegisterAnInstanceOfEachOfTheRequiredPluginServicesAfterOSGiFrameworkIsInitialized() {
        spy.start();

        verify(bundleContext).registerService(eq(PluginRegistryService.class), any(DefaultPluginRegistryService.class), isNull());
        verify(bundleContext).registerService(eq(LoggingService.class), any(DefaultPluginLoggingService.class), isNull());
    }

    @Test
    void doOnShouldRunAnActionOnSpecifiedPluginImplementationsOfAGivenInterface() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        registerService(firstService, "plugin-one", "extension-one");
        registerService(secondService, "plugin-two", "extension-two");
        spy.start();

        spy.doOn(SomeInterface.class, "plugin-two", "extension-two", (obj, pluginDescriptor) -> {
            assertThat(pluginDescriptor).isEqualTo(buildExpectedDescriptor("plugin-two"));
            return obj.someMethodWithReturn();
        });

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService).someMethodWithReturn();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    void doOnShouldThrowAnExceptionWhenThereAreMultipleServicesWithSamePluginIdAndSameExtensionType_IdeallyThisShouldNotHappenInProduction() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        String pluginID = "same_symbolic_name";
        registerServicesWithSamePluginID(pluginID, "test-extension", firstService, secondService);
        spy.start();

        try {
            spy.doOn(SomeInterface.class, pluginID, "test-extension", (obj, pluginDescriptor) -> {
                assertThat(pluginDescriptor).isEqualTo(buildExpectedDescriptor(pluginID));
                return obj.someMethodWithReturn();
            });
            fail("Should throw plugin framework exception");
        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("More than one reference found")).isTrue();
            assertThat(ex.getMessage().contains(SomeInterface.class.getCanonicalName())).isTrue();
            assertThat(ex.getMessage().contains(pluginID)).isTrue();
        }

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    void doOnShouldThrowAnExceptionWhenNoServicesAreFoundForTheGivenFilterAndServiceReference() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);

        String pluginID = "dummy_symbolic_name";
        registerServicesWithSamePluginID(pluginID, "test-extension", firstService, secondService);
        spy.start();

        try {
            spy.doOn(SomeOtherInterface.class, pluginID, "test-extension", (obj, pluginDescriptor) -> {
                assertThat(pluginDescriptor).isEqualTo(buildExpectedDescriptor(pluginID));
                throw new RuntimeException("Should Not Be invoked");
            });
            fail("Should throw plugin framework exception");

        } catch (GoPluginFrameworkException ex) {
            assertThat(ex.getMessage().startsWith("No reference found")).isTrue();
            assertThat(ex.getMessage().contains(SomeOtherInterface.class.getCanonicalName())).isTrue();
            assertThat(ex.getMessage().contains(pluginID)).isTrue();
        }

        verify(firstService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethodWithReturn();
        verify(secondService, never()).someMethod();
        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    void hasReferencesShouldReturnAppropriateValueIfSpecifiedPluginImplementationsOfAGivenInterfaceIsFoundOrNotFound() throws Exception {
        SomeInterface firstService = mock(SomeInterface.class);
        SomeInterface secondService = mock(SomeInterface.class);
        spy.start();

        boolean reference = spy.hasReferenceFor(SomeInterface.class, secondService.toString(), "extension-two");

        assertThat(reference).isFalse();

        registerService(firstService, "plugin-one", "extension-one");
        registerService(secondService, "plugin-two", "extension-two");

        reference = spy.hasReferenceFor(SomeInterface.class, "plugin-two", "extension-two");
        assertThat(reference).isTrue();

        verifyNoMoreInteractions(firstService, secondService);
    }

    @Test
    void shouldUnloadAPlugin() throws BundleException {
        GoPluginBundleDescriptor pluginDescriptor = mock(GoPluginBundleDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);

        spy.unloadPlugin(pluginDescriptor);

        verify(bundle, atLeastOnce()).stop();
        verify(bundle, atLeastOnce()).uninstall();
    }

    @Test
    void shouldNotFailToUnloadAPluginWhoseBundleIsNull() {
        GoPluginBundleDescriptor pluginDescriptor = mock(GoPluginBundleDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(null);

        try {
            spy.unloadPlugin(pluginDescriptor);
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
    }

    @Nested
    class GetExtensionsInfoFromThePlugin {
        @Test
        void shouldGetExtensionsInfoForThePlugin() throws Exception {
            when(registry.getPlugin(anyString())).thenReturn(mock(GoPluginDescriptor.class));

            GoPlugin firstService = mock(GoPlugin.class);
            GoPlugin secondService = mock(GoPlugin.class);

            final GoPluginIdentifier firstPluginIdentifier = mock(GoPluginIdentifier.class);
            when(firstService.pluginIdentifier()).thenReturn(firstPluginIdentifier);
            when(firstPluginIdentifier.getExtension()).thenReturn("elastic-agent");
            when(firstPluginIdentifier.getSupportedExtensionVersions()).thenReturn(List.of("1.0", "2.0"));

            final GoPluginIdentifier secondPluginIdentifier = mock(GoPluginIdentifier.class);
            when(secondService.pluginIdentifier()).thenReturn(secondPluginIdentifier);
            when(secondPluginIdentifier.getExtension()).thenReturn("authorization");
            when(secondPluginIdentifier.getSupportedExtensionVersions()).thenReturn(List.of("1.0"));

            registerService("plugin-one", firstService, secondService);
            spy.start();

            final Map<String, List<String>> info = spy.getExtensionsInfoFromThePlugin("plugin-one");

            assertThat(info).hasSize(2)
                    .containsEntry("elastic-agent", List.of("1.0", "2.0"))
                    .containsEntry("authorization", List.of("1.0"));
        }
    }

    @Test
    void shouldSkipUninstallIfPluginIsPreviouslyUninstalled() throws BundleException {
        GoPluginBundleDescriptor pluginDescriptor = mock(GoPluginBundleDescriptor.class);
        when(pluginDescriptor.bundle()).thenReturn(bundle);
        when(bundle.getState()).thenReturn(Bundle.UNINSTALLED);

        spy.unloadPlugin(pluginDescriptor);

        verify(bundle, never()).start();
        verify(bundle, never()).uninstall();
    }

    private void registerServicesWithSamePluginID(String pluginID, String extensionType, SomeInterface... someInterfaces) throws InvalidSyntaxException {
        ArrayList<ServiceReference<SomeInterface>> references = new ArrayList<>();

        for (SomeInterface someInterface : someInterfaces) {
            ServiceReference<SomeInterface> reference = mock(ServiceReference.class);
            Bundle bundle = mock(Bundle.class);
            lenient().when(reference.getBundle()).thenReturn(bundle);
            lenient().when(bundleContext.getService(reference)).thenReturn(someInterface);
            references.add(reference);
        }

        String propertyFormat = String.format("(&(%s=%s)(%s=%s))", "PLUGIN_ID", pluginID, Constants.BUNDLE_CATEGORY, extensionType);
        lenient().when(bundleContext.getServiceReferences(SomeInterface.class, propertyFormat)).thenReturn(references);
        when(registry.getPlugin(pluginID)).thenReturn(buildExpectedDescriptor(pluginID));
    }

    private void registerService(SomeInterface someInterface, String pluginID, String extension) throws InvalidSyntaxException {
        ServiceReference<SomeInterface> reference = mock(ServiceReference.class);

        lenient().when(reference.getBundle()).thenReturn(bundle);
        lenient().when(bundleContext.getService(reference)).thenReturn(someInterface);
        lenient().when(registry.getPlugin(pluginID)).thenReturn(buildExpectedDescriptor(pluginID));

        String propertyFormat = String.format("(&(%s=%s)(%s=%s))", "PLUGIN_ID", pluginID, Constants.BUNDLE_CATEGORY, extension);
        lenient().when(bundleContext.getServiceReferences(SomeInterface.class, propertyFormat)).thenReturn(List.of(reference));
        lenient().when(bundleContext.getServiceReferences(SomeInterface.class, null)).thenReturn(List.of(reference));
    }

    private void registerService(String pluginID, GoPlugin... someInterfaces) throws InvalidSyntaxException {
        final List<ServiceReference<GoPlugin>> serviceReferences = Arrays.stream(someInterfaces).map(someInterface -> {
            ServiceReference<GoPlugin> reference = mock(ServiceReference.class);
            lenient().when(reference.getBundle()).thenReturn(bundle);
            when(bundleContext.getService(reference)).thenReturn(someInterface);
            when(registry.getPlugin(pluginID)).thenReturn(buildExpectedDescriptor(pluginID));
            return reference;
        }).collect(Collectors.toList());

        String propertyFormat = String.format("(&(%s=%s))", "PLUGIN_ID", pluginID);
        when(bundleContext.getServiceReferences(GoPlugin.class, propertyFormat)).thenReturn(serviceReferences);
    }

    private GoPluginDescriptor buildExpectedDescriptor(String pluginID) {
        return GoPluginDescriptor.builder().id(pluginID)
                .version("1")
                .about(GoPluginDescriptor.About.builder()
                        .name("Plugin Descriptor Validator")
                        .version("1.0.1")
                        .targetGoVersion("17.12")
                        .description("Validates its own plugin descriptor")
                        .vendor(new GoPluginDescriptor.Vendor("GoCD Team", "https://gocd.org"))
                        .targetOperatingSystems(List.of("Linux", "Windows"))
                        .build()
                ).isBundledPlugin(true)
                .build();
    }

    private interface SomeInterface {
        void someMethod();

        Object someMethodWithReturn();
    }

    private interface SomeOtherInterface {
    }
}
