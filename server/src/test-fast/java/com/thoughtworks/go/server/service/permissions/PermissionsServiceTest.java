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

package com.thoughtworks.go.server.service.permissions;

import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrincipal;
import com.thoughtworks.go.server.service.permissions.entity.ConfigRepoPermission;
import com.thoughtworks.go.server.service.permissions.entity.EnvironmentPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionsServiceTest {
    private Username username;
    private PermissionsService service;

    @Mock
    private EnvironmentPermission environmentPermission;
    @Mock
    private ConfigRepoPermission configRepoPermission;

    @BeforeEach
    void setUp() {

        username = new Username("Bob" + UUID.randomUUID());
        service = new PermissionsService(environmentPermission, configRepoPermission);

        when(environmentPermission.name()).thenReturn("environment");
        when(configRepoPermission.name()).thenReturn("config_repo");

        SessionUtils.setCurrentUser(new GoUserPrincipal(username.getUsername().toString(), username.getUsername().toString(), GoAuthority.ROLE_ANONYMOUS.asAuthority()));
    }

    @Test
    void shouldReturnAllTheEntityNamesThatSupportsPermission() {
        assertThat(service.allEntitiesSupportsPermission()).isEqualTo(List.of("environment", "config_repo"));
    }

    @Test
    void shouldFetchPermissions() {
        Map<String, Object> environments = new LinkedHashMap<>();
        environments.put("view", List.of("QA", "UAT"));
        environments.put("administer", List.of("QA"));

        Map<String, Object> configRepo = new LinkedHashMap<>();
        configRepo.put("view", List.of("repo1", "repo2"));
        configRepo.put("administer", List.of("repo1"));

        when(environmentPermission.permissions(username)).thenReturn(environments);
        when(configRepoPermission.permissions(username)).thenReturn(configRepo);

        Map<String, Object> permissions = service.getPermissions(service.allEntitiesSupportsPermission());

        assertThat(permissions.get("environment")).isEqualTo(environments);
        assertThat(permissions.get("config_repo")).isEqualTo(configRepo);
    }

    @Test
    void shouldFetchPermissionsOnlyForRequestedType() {
        Map<String, Object> environments = new LinkedHashMap<>();
        environments.put("view", List.of("QA", "UAT"));
        environments.put("administer", List.of("QA"));

        when(environmentPermission.permissions(username)).thenReturn(environments);

        Map<String, Object> permissions = service.getPermissions(List.of("environment"));

        assertThat(permissions.get("environment")).isEqualTo(environments);
    }

    @Test
    void shouldNotAddEntitiesWhenNotRequested() {
        Map<String, Object> permissions = service.getPermissions(Collections.emptyList());

        assertThat(permissions.get("environment")).isEqualTo(null);
        assertThat(permissions.get("config_repo")).isEqualTo(null);
    }
}
