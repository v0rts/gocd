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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.StageSummaryModel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ScheduleServiceCachedIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineService pipelineService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigService goConfigService;
    @Autowired private StageService stageService;
    @Autowired private SchedulingCheckerService schedulingChecker;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private StageDao stageDao;
    @Autowired private StageOrderService stageOrderService;
    @Autowired private SecurityService securityService;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private AgentAssignment agentAssignment;
    @Autowired private EnvironmentConfigService environmentConfigService;
    @Autowired private PipelineLockService pipelineLockService;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private AgentService agentService;
    @Autowired private TransactionSynchronizationManager synchronizationManager;

    private PipelineWithTwoStages preCondition;
    private static final GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        preCondition = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        preCondition.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        preCondition.onTearDown();
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
    }

    @Test
    // #2296
    public void shouldUseLatestStageStateInsteadOfCachedWhenScheduling() throws Exception {
        assertThat(stageService.isStageActive(preCondition.pipelineName, preCondition.devStage), is(false));

        Pipeline pipeline0 = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);

        Pipeline pipeline1 = tryToScheduleAPipeline();
        assertThat("we should be able to schedule a pipeline when its first stage is inactive",
                pipeline0.getId(), is(not(pipeline1.getId())));

        Pipeline pipeline2 = tryToScheduleAPipeline();
        assertThat("we should NOT schedule the pipeline again when its first stage is active",
                pipeline2.getId(), is(pipeline1.getId()));
    }

    @Test
    public void shouldUpdateResultOfStageWhenJobCompletes() throws Exception {
        Pipeline assigned = preCondition.createPipelineWithFirstStageAssigned();

        Stage stage = assigned.findStage(preCondition.devStage);
        StageSummaryModel model = stageService.findStageSummaryByIdentifier(stage.getIdentifier(), new Username(new CaseInsensitiveString("foo")), new HttpLocalizedOperationResult());
        assertThat(model.getStage().getFirstJob().getState(), is(JobState.Assigned));
        scheduleService.updateJobStatus(stage.getFirstJob().getIdentifier(), JobState.Building);
        StageSummaryModel reloadedModel = stageService.findStageSummaryByIdentifier(stage.getIdentifier(), new Username(new CaseInsensitiveString("foo")), new HttpLocalizedOperationResult());
        assertThat(reloadedModel.getStage().getFirstJob().getState(), is(JobState.Building));
    }

    @Test
    public void shouldUpdateResultOfStageWhenJobCompletes_irrespectiveOfOtherThreadsPrimingStageCache() throws Exception {
        Pipeline assigned = preCondition.createPipelineWithFirstStageAssigned();

        Stage stage = assigned.findStage(preCondition.devStage);

        JobIdentifier identifier = stage.getFirstJob().getIdentifier();
        scheduleService.updateJobStatus(identifier, JobState.Building);
        scheduleService.jobCompleting(identifier, JobResult.Passed, "uuid");

        stageDao.stageById(stage.getId()); //priming the cache

        scheduleService.updateJobStatus(stage.getFirstJob().getIdentifier(), JobState.Completed);

        StageSummaryModel reloadedModel = stageService.findStageSummaryByIdentifier(stage.getIdentifier(), new Username(new CaseInsensitiveString("foo")), new HttpLocalizedOperationResult());
        Stage reloadedStage = reloadedModel.getStage();
        assertThat(reloadedStage.getFirstJob().getState(), is(JobState.Completed));
        assertThat(reloadedStage.getCompletedByTransitionId(), is(reloadedStage.getFirstJob().getTransitions().byState(JobState.Completed).getId()));
        assertThat(reloadedStage.getResult(), is(StageResult.Passed));
        assertThat(reloadedStage.getState(), is(StageState.Passed));
    }

    @Test
    public void shouldUpdateResultOfStageWhenJobCompletesOnTransactionCommitOnly() throws Exception {
        StageService stageService = mock(StageService.class);
        StageDao stageDao = mock(StageDao.class);
        SchedulingPerformanceLogger schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);

        ScheduleService service = new ScheduleService(goConfigService, pipelineService, stageService, schedulingChecker, pipelineDao,
                stageDao, stageOrderService, securityService, pipelineScheduleQueue, jobInstanceService, jobInstanceDao, agentAssignment, environmentConfigService,
                pipelineLockService, serverHealthService, transactionTemplate, agentService, synchronizationManager, null, null, null, null, schedulingPerformanceLogger,
                null,null
        );

        Pipeline assigned = preCondition.createPipelineWithFirstStageAssigned();

        Stage stage = assigned.findStage(preCondition.devStage);

        when(stageService.stageById(stage.getId())).thenThrow(new RuntimeException("find fails"));

        try {
            service.updateJobStatus(stage.getFirstJob().getIdentifier(), JobState.Completed);
            fail("should have failed because stage lookup bombed");
        } catch (Exception e) {
            //ignore
        }
        verify(stageDao, never()).clearCachedAllStages(stage.getIdentifier().getPipelineName(), stage.getIdentifier().getPipelineCounter(), stage.getName());
    }

    private Pipeline tryToScheduleAPipeline() {
        BuildCause buildCause = BuildCause.createWithModifications(modifyOneFile(preCondition.pipelineConfig()), "");
        dbHelper.saveMaterials(buildCause.getMaterialRevisions());
        pipelineScheduleQueue.schedule(new CaseInsensitiveString(preCondition.pipelineName), buildCause);
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        return pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
    }
}
