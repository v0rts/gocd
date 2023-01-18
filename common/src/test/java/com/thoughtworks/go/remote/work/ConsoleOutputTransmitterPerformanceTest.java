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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class ConsoleOutputTransmitterPerformanceTest {
    private static final int SECOND = 1000;

    @BeforeEach
    public void setUp() {
        new SystemEnvironment().setProperty(SystemEnvironment.INTERVAL, "1");
    }

    @AfterEach
    public void tearDown() {
        new SystemEnvironment().clearProperty(SystemEnvironment.INTERVAL);
    }

    @Test
    public void shouldNotBlockPublisherWhenSendingToServer() throws InterruptedException {
        SlowResource resource = new SlowResource();
        final ConsoleOutputTransmitter transmitter = new ConsoleOutputTransmitter(resource);

        int numberToSend = 4;
        int actuallySent = transmitData(transmitter, numberToSend);
        transmitter.stop();

        assertThat("Send should not block.", numberToSend, lessThanOrEqualTo(actuallySent));
    }

    private int transmitData(final ConsoleOutputTransmitter transmitter, final int numberOfSeconds)
            throws InterruptedException {
        final int[] count = {0};
        Thread thread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            count[0] = 0;
            while (System.currentTimeMillis() < startTime + numberOfSeconds * SECOND) {
                String line = "This is line " + count[0];
                transmitter.consumeLine(line);
                sleepFor(SECOND);
                count[0]++;
            }
        });
        thread.start();
        thread.join();
        return count[0];
    }

    private void sleepFor(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //ignore
        }
    }
}
