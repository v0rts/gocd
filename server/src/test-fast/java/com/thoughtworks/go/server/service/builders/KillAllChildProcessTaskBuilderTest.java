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
package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.util.ProcessWrapper;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class KillAllChildProcessTaskBuilderTest {
    private BuilderFactory builderFactory;

    @BeforeEach
    public void setUp() throws Exception {
        builderFactory = mock(BuilderFactory.class);
    }

    @Test
    @Timeout(value = 11, unit = MINUTES)
    public void shouldKillAllChildProcessOnBuild() throws Exception {
        ProcessWrapper processWrapper = platformSpecificSleepCommand().withArg(String.valueOf(10 * 60)).withEncoding(UTF_8).execute(ProcessOutputStreamConsumer.inMemoryConsumer(), new EnvironmentVariableContext(),
            null);//10 mins

        assertThat(processWrapper.isRunning(), is(true));

        DefaultGoPublisher publisher = mock(DefaultGoPublisher.class);
        EnvironmentVariableContext environmentVariableContext = mock(EnvironmentVariableContext.class);


        long before = getSystemTime();
        Builder builder = new KillAllChildProcessTaskBuilder().createBuilder(builderFactory, new KillAllChildProcessTask(), null, null);
        builder.build(publisher, environmentVariableContext, null, null, null, UTF_8);

        assertThat(processWrapper.waitForExit(), is(greaterThan(0)));
        assertThat(getSystemTime() - before, is(lessThan(10 * 60 * 1000 * 1000 * 1000L)));//min = 10; sec = 60*min; mills = 1000*sec; micro = 1000*mills; nano = 1000*micro;
    }

    private CommandLine platformSpecificSleepCommand() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return CommandLine.createCommandLine("powershell").withArg("sleep");
        }
        return CommandLine.createCommandLine("sleep");
    }

    @Test
    public void builderReturnedByThisTaskBuilderShouldBeSerializable() throws Exception {
        KillAllChildProcessTaskBuilder killAllChildProcessTaskBuilder = new KillAllChildProcessTaskBuilder();
        Builder builder = killAllChildProcessTaskBuilder.createBuilder(null, null, null, null);
        new ObjectOutputStream(OutputStream.nullOutputStream()).writeObject(builder);
    }

    public long getSystemTime() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ?
            (bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime()) : 0L;
    }
}
