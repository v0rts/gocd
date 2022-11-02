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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.http.mocks.HttpRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class DenyIfRefererIsNotFilesFilterTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() throws Exception {
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @Test
    void shouldDenyGoCDAccessForRequestsReferredFromArtifacts() throws Exception {
        request = HttpRequestBuilder.GET("/api/admin")
                .withHeader("Referer", "http://example.com/go/files/foo")
                .build();

        new DenyIfRefererIsNotFilesFilter().doFilter(request, response, chain);

        verifyNoMoreInteractions(chain);
    }

    @Test
    void shouldErrorOutWhenAppliedToFilesUrl() throws Exception {
        request = HttpRequestBuilder.GET("/files/file2")
                .withHeader("Referer", "http://example.com/go/files/file1")
                .build();

        assertThatThrownBy(() -> new DenyIfRefererIsNotFilesFilter().doFilter(request, response, chain))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Filter should not be invoked for `/files/` urls.");
    }

    @Test
    void shouldNotBailIfRefererParsingFails() throws ServletException, IOException {
        request = HttpRequestBuilder.GET("/auth/login")
                .withHeader("Referer", "bad uri//com.Slack")
                .build();

        new DenyIfRefererIsNotFilesFilter().doFilter(request, response, chain);
    }
}
