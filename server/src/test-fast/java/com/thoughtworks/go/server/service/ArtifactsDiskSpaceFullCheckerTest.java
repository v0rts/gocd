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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.fixture.ArtifactsDiskIsFull;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(ArtifactsDiskIsFull.class)
public class ArtifactsDiskSpaceFullCheckerTest {
    private GoConfigService goConfigService;
    private EmailSender emailSender;
    private ArtifactsDiskSpaceFullChecker checker;

    @BeforeEach
    public void setUp() {
        goConfigService = mockGoConfigServiceToHaveSiteUrl();
        when(goConfigService.artifactsDir()).thenReturn(new File("."));
        emailSender = mock(EmailSender.class);
        checker = new ArtifactsDiskSpaceFullChecker(new SystemEnvironment(), emailSender, goConfigService, new SystemDiskSpaceChecker());
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    public void shouldSendEmailOnlyOnce() {
        when(goConfigService.adminEmail()).thenReturn("admin@tw.com");
        checker.check(new ServerHealthStateOperationResult());
        checker.check(new ServerHealthStateOperationResult()); // call once more
        verify(emailSender).sendEmail(any(SendEmailMessage.class));
    }

    @Test
    public void shouldSendEmailAgainIfProblemOccursAgain() {
        when(goConfigService.adminEmail()).thenReturn("admin@tw.com");

        checker.check(new ServerHealthStateOperationResult()); //should send email this time
        new ArtifactsDiskIsFull().afterEach(null);

        checker.check(new ServerHealthStateOperationResult()); //should not send email

        new ArtifactsDiskIsFull().beforeEach(null);
        checker.check(new ServerHealthStateOperationResult()); //should send email again
        verify(emailSender, times(2)).sendEmail(any(SendEmailMessage.class));
    }

    public static GoConfigService mockGoConfigServiceToHaveSiteUrl() {
        CruiseConfig cruiseConfig = configWithSiteUrl();
        GoConfigService goConfigService = mock(GoConfigService.class);
        when(goConfigService.artifactsDir()).thenReturn(new File("."));
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.adminEmail()).thenReturn("admin@email.com");
        return goConfigService;
    }

    private static CruiseConfig configWithSiteUrl() {
        ServerConfig serverConfig = new ServerConfig(null, null, new SiteUrl("http://test.host"), new SecureSiteUrl("https://test.host"));
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setServerConfig(serverConfig);
        return cruiseConfig;
    }
}
