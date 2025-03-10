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
package com.thoughtworks.go.server.newsecurity.providers;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.*;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.newsecurity.models.AccessToken;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.security.userdetail.GoUserPrincipal;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.server.security.GoAuthority.ROLE_USER;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(ClearSingleton.class)
class WebBasedPluginAuthenticationProviderTest {
    private static final String PLUGIN_ID = "github.oauth";
    private static final AccessToken CREDENTIALS = new AccessToken(Map.of("access_token", "some-token"));
    private AuthorizationExtension authorizationExtension;
    private PluginRoleService pluginRoleService;
    private TestingClock clock;
    private WebBasedPluginAuthenticationProvider authenticationProvider;
    private SecurityConfig securityConfig;
    private UserService userService;
    private AuthorityGranter authorityGranter;
    private SecurityAuthConfig githubSecurityAuthconfig;
    private GoConfigService goConfigService;

    @BeforeEach
    void setUp() {
        SecurityService securityService = mock(SecurityService.class);
        authorityGranter = spy(new AuthorityGranter(securityService));
        goConfigService = mock(GoConfigService.class);
        userService = mock(UserService.class);

        authorizationExtension = mock(AuthorizationExtension.class);
        pluginRoleService = mock(PluginRoleService.class);
        clock = new TestingClock();

        securityConfig = new SecurityConfig();
        githubSecurityAuthconfig = new SecurityAuthConfig("github", PLUGIN_ID);
        securityConfig.securityAuthConfigs().add(githubSecurityAuthconfig);
        when(goConfigService.security()).thenReturn(securityConfig);
        addPluginSupportingWebBasedAuthentication(PLUGIN_ID);

        authenticationProvider = new WebBasedPluginAuthenticationProvider(authorizationExtension, authorityGranter, goConfigService, pluginRoleService, userService, clock);
    }

    @Nested
    class Authenticate {
        @Test
        void shouldAuthenticateUserAgainstTheSpecifiedPlugin() {
            PluginRoleConfig adminRole = new PluginRoleConfig("admin", "github", new ConfigurationProperty());
            securityConfig.addRole(adminRole);

            authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            verify(authorizationExtension).authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubSecurityAuthconfig), List.of(adminRole));
        }

        @Test
        void inCaseOfMultipleAuthConfigsOnSuccessfulAuthenticationShouldNotTryAuthenticatingUserUsingRemainingAuthConfig() {
            User user = new User("username", "displayname", "emailId");
            SecurityAuthConfig githubPublic = new SecurityAuthConfig("github_public", PLUGIN_ID);
            SecurityAuthConfig githubEnterprise = new SecurityAuthConfig("github_enterprise", PLUGIN_ID);
            PluginRoleConfig adminRole = new PluginRoleConfig("admin", githubPublic.getId(), new ConfigurationProperty());
            PluginRoleConfig operatorRole = new PluginRoleConfig("operator", githubEnterprise.getId(), new ConfigurationProperty());

            securityConfig.securityAuthConfigs().clear();
            securityConfig.securityAuthConfigs().add(githubPublic);
            securityConfig.securityAuthConfigs().add(githubEnterprise);
            securityConfig.addRole(adminRole);
            securityConfig.addRole(operatorRole);

            when(authorizationExtension.authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubPublic), List.of(adminRole))).thenReturn(new AuthenticationResponse(user, List.of("admin")));

            final AuthenticationToken<AccessToken> authenticationToken = authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            assertThat(authenticationToken.getCredentials()).isEqualTo(CREDENTIALS);
            assertThat(authenticationToken.getPluginId()).isEqualTo(PLUGIN_ID);
            assertThat(authenticationToken.isAuthenticated(clock, new SystemEnvironment()))
                    .isEqualTo(true);
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(ROLE_USER.asAuthority());

            verify(authorizationExtension).authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubPublic), List.of(adminRole));
            verify(authorizationExtension, never()).authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubEnterprise), List.of(operatorRole));
        }

        @Test
        void shouldCreateUserIfDoesNotExist() {
            final User user = new User("username", "displayname", "emailId");
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, List.of("admin"));
            when(authorizationExtension.authenticateUser(eq(PLUGIN_ID), anyMap(), anyList(), anyList())).thenReturn(authenticationResponse);

            authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            ArgumentCaptor<com.thoughtworks.go.domain.User> argumentCaptor = ArgumentCaptor.forClass(com.thoughtworks.go.domain.User.class);
            verify(userService).addOrUpdateUser(argumentCaptor.capture(), any());

            com.thoughtworks.go.domain.User capturedUser = argumentCaptor.getValue();
            assertThat(capturedUser.getUsername().getUsername().toString())
                    .isEqualTo(user.getUsername());
            assertThat(capturedUser.getDisplayName())
                    .isEqualTo(user.getDisplayName());
            assertThat(capturedUser.getEmail())
                    .isEqualTo(user.getEmailId());
        }

        @Test
        void shouldAssignRolesToUser() {
            final User user = new User("username", "displayname", "emailId");
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, List.of("admin"));

            when(authorizationExtension.authenticateUser(eq(PLUGIN_ID), anyMap(), anyList(), anyList())).thenReturn(authenticationResponse);
            authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            verify(pluginRoleService).updatePluginRoles(PLUGIN_ID, user.getUsername(), CaseInsensitiveString.list("admin"));
        }

        @Test
        void shouldReturnAuthenticationTokenOnSuccessfulAuthorization() {
            final User user = new User("username", "displayname", "emailId");
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, List.of("admin"));

            when(authorizationExtension.authenticateUser(eq(PLUGIN_ID), anyMap(), anyList(), anyList())).thenReturn(authenticationResponse);

            final AuthenticationToken<AccessToken> authenticationToken = authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            assertThat(authenticationToken.getCredentials()).isEqualTo(CREDENTIALS);
            assertThat(authenticationToken.getPluginId()).isEqualTo(PLUGIN_ID);
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(ROLE_USER.asAuthority());
            assertThat(authenticationToken.isAuthenticated(clock, new SystemEnvironment()))
                    .isEqualTo(true);
        }

        @Test
        void shouldReturnAuthTokenWithUserDetails() {
            final User user = new User("username", "displayname", "emailId");
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, List.of("admin"));

            when(authorizationExtension.authenticateUser(eq(PLUGIN_ID), anyMap(), anyList(), anyList())).thenReturn(authenticationResponse);

            final AuthenticationToken<AccessToken> authenticationToken = authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            assertThat(authenticationToken.getUser().getDisplayName()).isEqualTo(user.getDisplayName());
            assertThat(authenticationToken.getUser().getUsername()).isEqualTo(user.getUsername());
            assertThat(authenticationToken.getUser().getAuthorities())
                    .containsExactly(ROLE_USER.asAuthority());
        }

        @Test
        void shouldEnsureUserDetailsInAuthTokenHasDisplayName() {
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(new User("username", null, "email"), List.of("admin"));

            when(authorizationExtension.authenticateUser(eq(PLUGIN_ID), anyMap(), anyList(), anyList())).thenReturn(authenticationResponse);

            final AuthenticationToken<AccessToken> authenticationToken = authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            assertThat(authenticationToken.getUser().getDisplayName()).isEqualTo(authenticationResponse.getUser().getUsername());
        }

        @Test
        void shouldAssignRoleBeforeGrantingAnAuthority() {
            final User user = new User("username", null, "email");
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, List.of("admin"));
            when(authorizationExtension.authenticateUser(eq(PLUGIN_ID), anyMap(), anyList(), anyList())).thenReturn(authenticationResponse);

            final InOrder inOrder = inOrder(pluginRoleService, authorityGranter);

            authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            inOrder.verify(pluginRoleService).updatePluginRoles(PLUGIN_ID, user.getUsername(), List.of(new CaseInsensitiveString("admin")));
            inOrder.verify(authorityGranter).authorities(user.getUsername());
        }

        @Test
        void shouldPerformOperationInSequence() {
            final User user = new User("username", null, "email");
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, List.of("admin"));
            when(authorizationExtension.authenticateUser(eq(PLUGIN_ID), anyMap(), anyList(), anyList())).thenReturn(authenticationResponse);

            final InOrder inOrder = inOrder(authorizationExtension, pluginRoleService, authorityGranter, userService);

            authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID);

            inOrder.verify(authorizationExtension).authenticateUser(eq(PLUGIN_ID), eq(CREDENTIALS.getCredentials()), anyList(), anyList());
            inOrder.verify(userService).addOrUpdateUser(any(com.thoughtworks.go.domain.User.class), eq(githubSecurityAuthconfig));
            inOrder.verify(pluginRoleService).updatePluginRoles(PLUGIN_ID, user.getUsername(), List.of(new CaseInsensitiveString("admin")));
            inOrder.verify(authorityGranter).authorities(user.getUsername());
        }

        @Test
        void shouldErrorOutWhenAutoRegistrationOfNewUserIsDisabledByAdmin() {
            final User user = new User("username", null, "email");
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, List.of("admin"));

            when(authorizationExtension.authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubSecurityAuthconfig), emptyList())).thenReturn(authenticationResponse);
            doThrow(new OnlyKnownUsersAllowedException("username", "Please ask the administrator to add you to GoCD.")).when(userService).addOrUpdateUser(any(), any());

            assertThatThrownBy(() -> authenticationProvider.authenticate(CREDENTIALS, PLUGIN_ID))
                    .isInstanceOf(OnlyKnownUsersAllowedException.class)
                    .hasMessageContaining("Please ask the administrator to add you to GoCD.");
        }
    }

    @Nested
    class ReAuthenticate {
        @Test
        void shouldReAuthenticateUserUsingAuthenticationToken() {
            final GoUserPrincipal user = new GoUserPrincipal("bob", "Bob");
            final AuthenticationToken<AccessToken> oldAuthenticationToken = new AuthenticationToken<>(user, CREDENTIALS, PLUGIN_ID, clock.currentTimeMillis(), "github");

            authenticationProvider.reauthenticate(oldAuthenticationToken);

            verify(authorizationExtension).authenticateUser(eq(PLUGIN_ID), eq(CREDENTIALS.getCredentials()), eq(List.of(githubSecurityAuthconfig)), anyList());
        }

        @Test
        void shouldReturnNullInCaseOfErrors() {
            final GoUserPrincipal user = new GoUserPrincipal("bob", "Bob");
            final AuthenticationToken<AccessToken> oldAuthenticationToken = new AuthenticationToken<>(user, CREDENTIALS, PLUGIN_ID, clock.currentTimeMillis(), "github");
            when(authorizationExtension.authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubSecurityAuthconfig), emptyList()))
                    .thenReturn(null);

            final AuthenticationToken<AccessToken> authenticationToken = authenticationProvider.reauthenticate(oldAuthenticationToken);

            assertThat(authenticationToken).isNull();
        }

        @Test
        void shouldTryToReAuthenticateUserAgainWhenPreviouslyAuthenticatedAuthConfigForThePluginIsDeleted() {
            final GoUserPrincipal user = new GoUserPrincipal("bob", "Bob");
            securityConfig.securityAuthConfigs().remove(githubSecurityAuthconfig);
            final SecurityAuthConfig githubPrivateSecurityConfig = new SecurityAuthConfig("github-private", PLUGIN_ID);
            securityConfig.securityAuthConfigs().add(githubPrivateSecurityConfig);

            final AuthenticationToken<AccessToken> oldAuthenticationToken = new AuthenticationToken<>(user, CREDENTIALS, PLUGIN_ID, clock.currentTimeMillis(), "github");

            authenticationProvider.reauthenticate(oldAuthenticationToken);

            verify(authorizationExtension, never()).authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubSecurityAuthconfig), emptyList());
            verify(authorizationExtension).authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubPrivateSecurityConfig), emptyList());
        }

        @Test
        void shouldErrorOutWhenAutoRegistrationOfNewUserIsDisabledByAdmin() {
            final GoUserPrincipal goUserPrincipal = new GoUserPrincipal("bob", "Bob");
            final AuthenticationToken<AccessToken> oldAuthenticationToken = new AuthenticationToken<>(goUserPrincipal, CREDENTIALS, PLUGIN_ID, clock.currentTimeMillis(), "github");

            final User user = new User("username", null, "email");
            AuthenticationResponse authenticationResponse = new AuthenticationResponse(user, List.of("admin"));

            when(authorizationExtension.authenticateUser(PLUGIN_ID, CREDENTIALS.getCredentials(), List.of(githubSecurityAuthconfig), emptyList())).thenReturn(authenticationResponse);
            doThrow(new OnlyKnownUsersAllowedException("username", "Please ask the administrator to add you to GoCD.")).when(userService).addOrUpdateUser(any(), any());

            assertThatThrownBy(() -> authenticationProvider.reauthenticate(oldAuthenticationToken))
                    .isInstanceOf(OnlyKnownUsersAllowedException.class)
                    .hasMessageContaining("Please ask the administrator to add you to GoCD.");
        }

    }

    @Test
    void shouldGetAuthorizationServerUrlWithConfiguredSecureSiteUrl() {
        final ServerConfig serverConfig = mock(ServerConfig.class);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(serverConfig.hasAnyUrlConfigured()).thenReturn(true);
        when(serverConfig.getSiteUrlPreferablySecured()).thenReturn(new SecureSiteUrl("https://foo.bar.com"));

        authenticationProvider.getAuthorizationServerUrl(PLUGIN_ID, "https://example.com");

        verify(authorizationExtension, never()).getAuthorizationServerUrl(PLUGIN_ID, List.of(githubSecurityAuthconfig), "https://example.com");
        verify(authorizationExtension).getAuthorizationServerUrl(PLUGIN_ID, List.of(githubSecurityAuthconfig), "https://foo.bar.com");
    }

    @Test
    void shouldGetAuthorizationServerUrl() {
        final ServerConfig serverConfig = mock(ServerConfig.class);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(serverConfig.hasAnyUrlConfigured()).thenReturn(false);

        authenticationProvider.getAuthorizationServerUrl(PLUGIN_ID, "https://example.com");

        verify(authorizationExtension).getAuthorizationServerUrl(PLUGIN_ID, List.of(githubSecurityAuthconfig), "https://example.com");
    }

    @Test
    void shouldThrowUsefulErrorIfAuthorizationServerUrlIsBad() {
        final ServerConfig serverConfig = mock(ServerConfig.class);
        when(goConfigService.serverConfig()).thenReturn(serverConfig);
        when(serverConfig.hasAnyUrlConfigured()).thenReturn(true);
        when(serverConfig.getSiteUrlPreferablySecured()).thenReturn(new SecureSiteUrl("https://badurl:3434:"));

        assertThatThrownBy(() -> authenticationProvider.getAuthorizationServerUrl(PLUGIN_ID, "https://example.com"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("does not appear to be a valid URL")
            .hasCauseInstanceOf(MalformedURLException.class);
    }

    @Test
    void shouldFetchAccessTokenFromPlugin() {
        when(authorizationExtension.fetchAccessToken(PLUGIN_ID, emptyMap(), Map.of("code", "some-code"), Map.of("auth", "context"), List.of(githubSecurityAuthconfig))).thenReturn(Map.of("access_token", "some-access-token"));

        final AccessToken accessToken = authenticationProvider.fetchAccessToken(PLUGIN_ID, emptyMap(), Map.of("code", "some-code"), Map.of("auth", "context"));

        assertThat(accessToken.getCredentials())
                .containsEntry("access_token", "some-access-token")
                .hasSize(1);
    }

    private void addPluginSupportingWebBasedAuthentication(String pluginId) {
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(
                GoPluginDescriptor.builder().id(pluginId).build(), null, null, null,
                new Capabilities(SupportedAuthType.Web, true, false, false));
        AuthorizationMetadataStore.instance().setPluginInfo(pluginInfo);
    }
}
