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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class UpdateAgentStatusTest {
    @Autowired
    private AgentService agentService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MaterialRepository materialRepository;

    private PipelineWithTwoStages preCondition;
    private static final GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        preCondition = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        preCondition.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        agentService.clearAll();
        agentService.saveOrUpdate(new Agent("uuid", "CCEDev01", "10.81.2.1", "cookie"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        preCondition.onTearDown();
    }

    @Test
    public void shouldUpdateAgentIPAddressWhenItChanges_asAgent() {
        String oldIp = agentService.getAgentByUUID("uuid").getIpaddress();
        assertThat(oldIp).isEqualTo("10.81.2.1");

        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));

        agentService.updateRuntimeInfo(agentRuntimeInfo1);

        String newIp = agentService.getAgentByUUID("uuid").getIpaddress();
        assertThat(newIp).isEqualTo("10.18.3.95");
    }

    @Test
    public void shouldUpdateAgentWorkingDirWhenItChanges() {
        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));
        agentRuntimeInfo1.setLocation("/myDirectory");

        agentService.updateRuntimeInfo(agentRuntimeInfo1);

        assertThat(agentService.findAgentAndRefreshStatus("uuid").getLocation()).isEqualTo("/myDirectory");
    }


    @Test
    public void shouldLogWarningWhenIPAddressChanges() {
        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));
        agentRuntimeInfo1.setLocation("/myDirectory");

        try (LogFixture logging = logFixtureFor(AgentService.class, Level.DEBUG)) {
            agentService.updateRuntimeInfo(agentRuntimeInfo1);
            assertThat(logging.getLog()).contains("Agent with UUID [uuid] changed IP Address from [10.81.2.1] to [10.18.3.95]");
        }
    }
}



