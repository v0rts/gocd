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

package com.thoughtworks.go.apiv1.webhook.request.payload.push;

import com.thoughtworks.go.apiv1.webhook.request.json.GitHubRepository;

import java.util.Set;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.removeStart;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class GitHubPush implements PushPayload {
    private String ref;

    private GitHubRepository repository;

    @Override
    public Set<String> branches() {
        return ref.startsWith("refs/heads/") ? Set.of(removeStart(ref, "refs/heads/")) : emptySet();
    }

    @Override
    public String hostname() {
        return repository.hostname();
    }

    @Override
    public String fullName() {
        return repository.fullName();
    }

}
