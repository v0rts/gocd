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
import com.thoughtworks.go.apiv1.webhook.request.payload.push.BitbucketPush;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BitbucketRequestTest implements WithMockRequests {
    @ParameterizedTest
    @FileSource(files = "/bitbucket-push.json")
    void parsePayload(String body) {
        final BitbucketRequest req = bitbucket().body(body).build();

        assertPayload(Set.of("release"), req.parsePayload(BitbucketPush.class));
    }

    @ParameterizedTest
    @FileSource(files = "/bitbucket-push-multiple-changes.json")
    void parsePayloadWithMultipleChanges(String body) {
        assertPayload(Set.of("release/1.0", "release/2.0", "release/3.0"),
                bitbucket().body(body).build().parsePayload(BitbucketPush.class));
    }

    @Test
    void authTokenHandlesUserInfoFormat() {
        assertEquals("webhook-secret", bitbucket().auth("webhook-secret").build().authToken());
        assertEquals("webhook-secret", bitbucket().
                auth("webhook-secret:and ignore everything after the first colon:pls:kthx").build().authToken());
    }

    private void assertPayload(Set<String> expectedBranches, final BitbucketPush payload) {
        assertEquals(expectedBranches, payload.branches());
        assertEquals("gocd/spaceship", payload.fullName());
        assertEquals("bitbucket.org", payload.hostname());
        assertEquals("git", payload.scmType());
    }
}
