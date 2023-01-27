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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.thoughtworks.go.domain.AgentRuntimeStatus.Idle;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class AgentRuntimeInfoTest {
    private File pipelinesFolder;

    @BeforeEach
    public void setup() throws Exception {
        pipelinesFolder = new File("pipelines");
        pipelinesFolder.mkdirs();
    }

    @AfterEach
    public void teardown() throws Exception {
        FileUtils.deleteQuietly(pipelinesFolder);
    }

    @Test
    public void shouldThrowOnEmptyLocation() {
        assertThatThrownBy(() -> AgentRuntimeInfo.fromServer(new Agent("uuid", "localhost", "127.0.0.1"), false, "", 0L, "linux"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessageContaining("Agent should not register without installation path");
    }

    @Test
    public void shouldUsingIdleWhenRegistrationRequestIsFromLocalAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new Agent("uuid", "localhost", "127.0.0.1"), false, "/var/lib", 0L, "linux");

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(Idle));
    }

    @Test
    public void shouldBeUnknownWhenRegistrationRequestIsFromLocalAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new Agent("uuid", "localhost", "176.19.4.1"), false, "/var/lib", 0L, "linux");

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Unknown));
    }

    @Test
    public void shouldUsingIdleWhenRegistrationRequestIsFromAlreadyRegisteredAgent() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(
                new Agent("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");

        assertThat(agentRuntimeInfo.getRuntimeStatus(), is(AgentRuntimeStatus.Idle));
    }

    @Test
    public void shouldNotMatchRuntimeInfosWithDifferentOperatingSystems() {
        AgentRuntimeInfo linux = AgentRuntimeInfo.fromServer(new Agent("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "linux");
        AgentRuntimeInfo osx = AgentRuntimeInfo.fromServer(new Agent("uuid", "localhost", "176.19.4.1"), true, "/var/lib", 0L, "foo bar");
        assertThat(linux, is(not(osx)));
    }

    @Test
    public void shouldInitializeTheFreeSpaceAtAgentSide() {
        AgentIdentifier id = new Agent("uuid", "localhost", "176.19.4.1").getAgentIdentifier();
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(id, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");

        assertThat(agentRuntimeInfo.getUsableSpace(), is(not(0L)));
    }

    @Test
    public void shouldNotBeLowDiskSpaceForMissingAgent() {
        assertThat(AgentRuntimeInfo.initialState(new Agent("uuid")).isLowDiskSpace(10L), is(false));
    }

    @Test
    public void shouldReturnTrueIfUsableSpaceLessThanLimit() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.initialState(new Agent("uuid"));
        agentRuntimeInfo.setUsableSpace(10L);
        assertThat(agentRuntimeInfo.isLowDiskSpace(20L), is(true));
    }

    @Test
    public void shouldHaveRelevantFieldsInDebugString() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        assertThat(agentRuntimeInfo.agentInfoDebugString(), is("Agent [localhost, 127.0.0.1, uuid, cookie]"));
    }

    @Test
    public void shouldHaveBeautifulPhigureLikeDisplayString() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.setLocation("/nim/appan/mane");
        assertThat(agentRuntimeInfo.agentInfoForDisplay(), is("Agent located at [localhost, 127.0.0.1, /nim/appan/mane]"));
    }

    @Test
    public void shouldTellIfHasCookie() {
        assertThat(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie").hasDuplicateCookie("cookie"), is(false));
        assertThat(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie").hasDuplicateCookie("different"), is(true));
        assertThat(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), null).hasDuplicateCookie("cookie"), is(false));
        assertThat(new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie").hasDuplicateCookie(null), is(false));
    }

    @Test
    public void shouldBeAbleToUpdateAgentAndAgentBootstrapperVersions() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), Idle, currentWorkingDirectory(), "cookie");

        agentRuntimeInfo.updateAgentVersion("20.5.0-2345").updateBootstrapperVersion("20.3.0-1234");
        assertThat(agentRuntimeInfo.getAgentVersion(), is("20.5.0-2345"));
        assertThat(agentRuntimeInfo.getAgentBootstrapperVersion(), is("20.3.0-1234"));
    }

    @Test
    public void shouldUpdateSelfForAnIdleAgent() {
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("localhost", "127.0.0.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), null);
        AgentRuntimeInfo newRuntimeInfo = new AgentRuntimeInfo(new AgentIdentifier("go02", "10.10.10.1", "uuid"), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        newRuntimeInfo.setBuildingInfo(new AgentBuildingInfo("Idle", ""));
        newRuntimeInfo.setLocation("home");
        newRuntimeInfo.setUsableSpace(10L);
        newRuntimeInfo.setOperatingSystem("Linux");
        newRuntimeInfo.updateAgentVersion("20.5.0-2345");
        newRuntimeInfo.updateBootstrapperVersion("20.3.0-1234");

        agentRuntimeInfo.updateSelf(newRuntimeInfo);

        assertThat(agentRuntimeInfo.getBuildingInfo(), is(newRuntimeInfo.getBuildingInfo()));
        assertThat(agentRuntimeInfo.getLocation(), is(newRuntimeInfo.getLocation()));
        assertThat(agentRuntimeInfo.getUsableSpace(), is(newRuntimeInfo.getUsableSpace()));
        assertThat(agentRuntimeInfo.getOperatingSystem(), is(newRuntimeInfo.getOperatingSystem()));
        assertThat(agentRuntimeInfo.getAgentVersion(), is("20.5.0-2345"));
        assertThat(agentRuntimeInfo.getAgentBootstrapperVersion(), is("20.3.0-1234"));
    }

}
