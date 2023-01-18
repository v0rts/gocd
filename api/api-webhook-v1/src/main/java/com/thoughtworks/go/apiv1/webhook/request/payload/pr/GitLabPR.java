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

package com.thoughtworks.go.apiv1.webhook.request.payload.pr;

import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.apiv1.webhook.request.json.GitLabProject;

import static com.thoughtworks.go.apiv1.webhook.request.payload.pr.PrPayload.State.CLOSED;
import static com.thoughtworks.go.apiv1.webhook.request.payload.pr.PrPayload.State.OPEN;
import static java.lang.String.format;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class GitLabPR implements PrPayload {
    private GitLabProject project;

    @SerializedName("object_attributes")
    private Attrs attrs;

    @Override
    public String identifier() {
        return format("#%d", attrs.number);
    }

    @Override
    public State state() {
        return "opened".equals(attrs.state) ? OPEN : CLOSED;
    }

    @Override
    public String hostname() {
        return project.hostname();
    }

    @Override
    public String fullName() {
        return project.fullName();
    }

    private static class Attrs {
        @SerializedName("iid")
        private int number;

        private String state;
    }
}
