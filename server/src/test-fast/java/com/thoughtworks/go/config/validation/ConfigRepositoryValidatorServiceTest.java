/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.service.ConfigRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ConfigRepositoryValidatorServiceTest {
    @Test
    public void shouldShutDownServerIfConfigRepositoryIsCorrupted() {
        ConfigRepository configRepository = mock(ConfigRepository.class);
        when(configRepository.isRepositoryCorrupted()).thenReturn(true);
        ConfigRepositoryValidatorService configRepositoryValidatorServiceSpy = spy(new ConfigRepositoryValidatorService(configRepository));
        doNothing().when(configRepositoryValidatorServiceSpy).shutDownServer();
        configRepositoryValidatorServiceSpy.afterPropertiesSet();
        verify(configRepositoryValidatorServiceSpy).shutDownServer();
    }

    @Test
    public void shouldNotShutDownServerIfConfigRepositoryIsSane() {
        ConfigRepository configRepository = mock(ConfigRepository.class);
        when(configRepository.isRepositoryCorrupted()).thenReturn(false);
        ConfigRepositoryValidatorService configRepositoryValidatorServiceSpy = spy(new ConfigRepositoryValidatorService(configRepository));
        doNothing().when(configRepositoryValidatorServiceSpy).shutDownServer();
        configRepositoryValidatorServiceSpy.afterPropertiesSet();
        verify(configRepositoryValidatorServiceSpy, never()).shutDownServer();
    }
}
