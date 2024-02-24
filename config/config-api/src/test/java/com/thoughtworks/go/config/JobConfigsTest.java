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
package com.thoughtworks.go.config;

import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class JobConfigsTest {

    @Test
    public void shouldAddJobsGivenInTheAttributesMapAfterClearingExistingJobs() {
        JobConfigs jobs = new JobConfigs();
        jobs.add(new JobConfig("quux"));
        jobs.setConfigAttributes(List.of(Map.of(JobConfig.NAME, "foo"), Map.of(JobConfig.NAME, "bar")));
        assertThat(jobs.get(0).name(), is(new CaseInsensitiveString("foo")));
        assertThat(jobs.get(1).name(), is(new CaseInsensitiveString("bar")));
        assertThat(jobs.size(), is(2));
    }

    @Test
    public void shouldNotFailForRepeatedJobNames_shouldInsteadSetErrorsOnValidation() {
        CruiseConfig config = new BasicCruiseConfig();
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline");
        config.addPipeline("grp", pipelineConfig);
        JobConfigs jobs = pipelineConfig.get(0).getJobs();
        jobs.add(new JobConfig("quux"));
        jobs.setConfigAttributes(List.of(Map.of(JobConfig.NAME, "foo"), Map.of(JobConfig.NAME, "foo")));
        assertThat(jobs.size(), is(2));

        JobConfig firstFoo = jobs.get(0);
        JobConfig secondFoo = jobs.get(1);
        assertThat(firstFoo.name(), is(new CaseInsensitiveString("foo")));
        assertThat(secondFoo.name(), is(new CaseInsensitiveString("foo")));

        assertThat(firstFoo.errors().isEmpty(), is(true));
        assertThat(secondFoo.errors().isEmpty(), is(true));
        jobs.validate(ConfigSaveValidationContext.forChain(config, config.getGroups(), config.getGroups().get(0), pipelineConfig, pipelineConfig.get(0), jobs));
        assertThat(firstFoo.errors().on(JobConfig.NAME), is("You have defined multiple jobs called 'foo'. Job names are case-insensitive and must be unique."));
        assertThat(secondFoo.errors().on(JobConfig.NAME), is("You have defined multiple jobs called 'foo'. Job names are case-insensitive and must be unique."));

    }

    @Test
    public void shouldValidateTree() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline");
        JobConfigs jobs = pipelineConfig.get(0).getJobs();
        jobs.add(new JobConfig("quux"));
        jobs.setConfigAttributes(List.of(Map.of(JobConfig.NAME, "foo"), Map.of(JobConfig.NAME, "foo")));
        assertThat(jobs.size(), is(2));

        JobConfig firstFoo = jobs.get(0);
        JobConfig secondFoo = jobs.get(1);
        assertThat(firstFoo.name(), is(new CaseInsensitiveString("foo")));
        assertThat(secondFoo.name(), is(new CaseInsensitiveString("foo")));

        assertThat(firstFoo.errors().isEmpty(), is(true));
        assertThat(secondFoo.errors().isEmpty(), is(true));
        jobs.validate(PipelineConfigSaveValidationContext.forChain(true, "group", pipelineConfig, pipelineConfig.get(0), jobs));
        assertThat(firstFoo.errors().on(JobConfig.NAME), is("You have defined multiple jobs called 'foo'. Job names are case-insensitive and must be unique."));
        assertThat(secondFoo.errors().on(JobConfig.NAME), is("You have defined multiple jobs called 'foo'. Job names are case-insensitive and must be unique."));

    }

    @Test
    public void shouldReturnTrueIfAllDescendentsAreValid() {
        JobConfig jobConfig = mock(JobConfig.class);
        when(jobConfig.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(true);
        JobConfigs jobConfigs = new JobConfigs(jobConfig);

        boolean isValid = jobConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()));
        assertTrue(isValid);

        verify(jobConfig).validateTree(any(PipelineConfigSaveValidationContext.class));
    }

    @Test
    public void shouldReturnFalseIfAnyDescendentIsInvalid() {
        JobConfig jobConfig = mock(JobConfig.class);
        when(jobConfig.validateTree(any(PipelineConfigSaveValidationContext.class))).thenReturn(false);
        JobConfigs jobConfigs = new JobConfigs(jobConfig);

        boolean isValid = jobConfigs.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig()));
        assertFalse(isValid);

        verify(jobConfig).validateTree(any(PipelineConfigSaveValidationContext.class));
    }

    @Test
    public void shouldClearExistingJobsWhenNullGivenAsAttributeMap() {
        JobConfigs jobs = new JobConfigs();
        jobs.add(new JobConfig("quux"));
        jobs.setConfigAttributes(null);
        assertThat(jobs.size(), is(0));
    }

    @Test
    public void shouldGetJobConfigByJobName() {
        JobConfigs configs = new JobConfigs();
        JobConfig expected = new JobConfig("job-1");
        configs.add(expected);
        configs.add(new JobConfig("job-2"));

        JobConfig actual = configs.getJob(new CaseInsensitiveString("job-1"));

        assertThat(actual, is(expected));
        assertThat(configs.getJob(new CaseInsensitiveString("some-junk")), is(nullValue()));
    }
}
