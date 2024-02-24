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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.util.Clock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeStampBasedCounterTest {
    @Test
    public void shouldGetIncrementalTimeStampBasedOnSeed() {
        Clock clock = mock(Clock.class);
        when(clock.currentTimeMillis()).thenReturn(1000L);
        TimeStampBasedCounter timeStampBasedCounter = new TimeStampBasedCounter(clock);
        assertThat(timeStampBasedCounter.getNext(), is(1001L));
        assertThat(timeStampBasedCounter.getNext(), is(1002L));
    }

    @Test
    public void shouldAllow() throws InterruptedException {
        ArrayList<Thread> list = new ArrayList<>();
        Clock clock = mock(Clock.class);
        when(clock.currentTimeMillis()).thenReturn(0L);
        TimeStampBasedCounter provider = new TimeStampBasedCounter(clock);
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(provider::getNext, "Thread" + i);
            list.add(thread);

        }
        for (Thread thread : list) {
            thread.start();
        }
        for (Thread thread : list) {
            thread.join();
        }
    }
}
