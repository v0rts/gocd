/*
 * Copyright 2022 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.rules;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.Validatable;

import java.util.List;

public interface RulesAware {
    List<String> allowedActions();

    List<String> allowedTypes();

    Rules getRules();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean canRefer(SupportedEntity supportedEntity, CaseInsensitiveString resource) {
        return canRefer(supportedEntity, resource.toString());
    }

    default boolean canRefer(SupportedEntity supportedEntity, String resource) {
        return canRefer(supportedEntity.getEntityType(), resource);
    }

    default boolean canRefer(Class<? extends Validatable> entityType, String resource) {
        Rules rules = getRules();
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        return rules.stream()
                .map(directive -> directive.apply("refer", entityType, resource))
                .filter(result -> result != Result.SKIP)
                .map(result -> result == Result.ALLOW)
                .findFirst()
                .orElse(false);
    }
}
