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
package com.thoughtworks.go.config;

import com.thoughtworks.go.ClearSingleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAs;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(ClearSingleton.class)
public class ConfigModifyingUserTest {
    @Test
    public void shouldIdentifyPassedInUserNameAsConfigModifyingUser() {
        ConfigModifyingUser user = new ConfigModifyingUser("loser_boozer");
        assertThat(user.getUserName(), is("loser_boozer"));
    }

    @Test
    public void shouldIdentifyLoggedInUserAsModifyingUser_WhenNoModifyingUserIsGiven() {
        loginAs("loser_boozer");
        ConfigModifyingUser user = new ConfigModifyingUser();
        assertThat(user.getUserName(), is("loser_boozer"));
    }
}
