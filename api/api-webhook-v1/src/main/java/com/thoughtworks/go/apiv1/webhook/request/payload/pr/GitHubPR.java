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

package com.thoughtworks.go.apiv1.webhook.request.payload.pr;

import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.apiv1.webhook.request.json.GitHubRepository;

import java.util.Set;

import static com.thoughtworks.go.apiv1.webhook.request.payload.pr.PrPayload.State.CLOSED;
import static com.thoughtworks.go.apiv1.webhook.request.payload.pr.PrPayload.State.OPEN;
import static java.lang.String.format;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class GitHubPR implements PrPayload {
    private static final Set<String> INTERESTING = Set.of("opened", "closed", "reopened");

    private GitHubRepository repository;

    private String action;

    @SerializedName("pull_request")
    private PR pr;

    public boolean isInteresting() {
        return INTERESTING.contains(action.toLowerCase());
    }

    public String action() {
        return action;
    }

    @Override
    public String identifier() {
        return format("#%d", pr.number);
    }

    @Override
    public State state() {
        return "open".equals(pr.state) ? OPEN : CLOSED;
    }

    @Override
    public String hostname() {
        return repository.hostname();
    }

    @Override
    public String fullName() {
        return repository.fullName();
    }

    private static class PR {
        private int number;

        private String state;
    }
}
