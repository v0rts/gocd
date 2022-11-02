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

package com.thoughtworks.go.apiv1.webhook.request;

import com.thoughtworks.go.apiv1.webhook.helpers.WithMockRequests;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.HostedBitbucketPush;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HostedBitbucketRequestTest implements WithMockRequests {
    @ParameterizedTest
    @FileSource(files = "/hosted-bitbucket-push.json")
    void parsePayload(String body) {
        assertPayload(Set.of("release"), hostedBitbucket().body(body).build().parsePayload(HostedBitbucketPush.class));
    }

    @ParameterizedTest
    @FileSource(files = "/hosted-bitbucket-push-multiple-changes.json")
    void parsePayloadWithMultipleChanges(String body) {
        assertPayload(Set.of("release/1.0", "release/2.0", "release/3.0"),
                hostedBitbucket().body(body).build().parsePayload(HostedBitbucketPush.class));
    }

    private void assertPayload(Set<String> expectedBranches, HostedBitbucketPush payload) {
        assertEquals(expectedBranches, payload.branches());
        assertEquals("gocd/spaceship", payload.fullName());
        assertEquals("bitbucket-server", payload.hostname());
        assertEquals("git", payload.scmType());
    }
}
