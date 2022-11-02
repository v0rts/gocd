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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helpers.GraphGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class FanInPerformanceTest {
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;
    @Autowired
    private GoConfigDao goConfigFileDao;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PipelineTimeline pipelineTimeline;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ScheduleTestUtil u;
    private GraphGenerator graphGenerator;

    @BeforeEach
    public void setup() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigFileDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        graphGenerator = new GraphGenerator(configHelper, u);
    }

    @AfterEach
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    @Timeout(value = 4, unit = MINUTES)
    public void shouldTestFanInForMesh() throws Exception {
        int numberOfNodesPerLevel = 10;
        int numberOfLevels = 10;
        int numberOfInstancesForUpstream = 1;

        ScmMaterial svn = u.wf((ScmMaterial) MaterialsMother.defaultMaterials().get(0), "folder1");
        String[] svn_revs = {"svn_1"};
        u.checkinInOrder(svn, svn_revs);

        PipelineConfig upstreamConfig = graphGenerator.createPipelineWithInstances("upstream", new ArrayList<>(), numberOfInstancesForUpstream);
        PipelineConfig currentConfig = graphGenerator.createMesh(upstreamConfig, "current", "up", numberOfInstancesForUpstream, numberOfNodesPerLevel, numberOfLevels);

        List<MaterialRevision> revisions = new ArrayList<>();
        revisions.add(u.mr(svn, true, "svn_1"));
        for (int i = 1; i <= numberOfNodesPerLevel; i++) {
            String pipelineName = String.format("pipeline_%s_%d_%d", "up", numberOfLevels, i);
            revisions.add(u.mr(new DependencyMaterial(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString("stage")), true, pipelineName + "/1/stage/1"));
        }
        MaterialRevisions given = new MaterialRevisions(revisions);

        long start = System.currentTimeMillis();
        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(currentConfig.name(), configHelper.currentConfig(), given);
        long timeTaken = (System.currentTimeMillis() - start) / 1000;
        assertThat(String.format("Fan-in took %ds. Should have finished in 10s.", timeTaken), timeTaken, Matchers.lessThan(10l));

        assertThat(finalRevisions, is(given));
    }

    private MaterialRevisions getRevisionsBasedOnDependencies(CaseInsensitiveString pipeline, CruiseConfig cruiseConfig, MaterialRevisions given) {
        pipelineTimeline.update();
        return pipelineService.getRevisionsBasedOnDependencies(given, cruiseConfig, pipeline);
    }
}
