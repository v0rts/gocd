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
package com.thoughtworks.go.util;

import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.util.StringUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class StringUtilTest {

    @Test
    public void shouldFindSimpleRegExMatch() {
        String url = "http://java.sun.com:80/docs/books/tutorial/essential/regex/test_harness.html";
        String baseUrl = StringUtil.matchPattern("^(http://[^/]*)/", url);
        assertThat(baseUrl, is("http://java.sun.com:80"));
    }

    @Test
    public void shouldHumanize() throws Exception {
        assertThat(humanize("camelCase"), is("camel case"));
        assertThat(humanize("camel"), is("camel"));
        assertThat(humanize("camelCaseForALongString"), is("camel case for a long string"));
    }

    @Test
    public void shouldStripTillLastOccurrenceOfGivenString() {
        assertThat(stripTillLastOccurrenceOf("HelloWorld@@\\nfoobar\\nquux@@keep_this", "@@"), is("keep_this"));
        assertThat(stripTillLastOccurrenceOf("HelloWorld", "@@"), is("HelloWorld"));
        assertThat(stripTillLastOccurrenceOf(null, "@@"), is(nullValue()));
        assertThat(stripTillLastOccurrenceOf("HelloWorld", null), is("HelloWorld"));
        assertThat(stripTillLastOccurrenceOf(null, null), is(nullValue()));
        assertThat(stripTillLastOccurrenceOf("", "@@"), is(""));
    }

    @Test
    public void shouldUnQuote() throws Exception {
        assertThat(unQuote("\"Hello World\""), is("Hello World"));
        assertThat(unQuote(null), is(nullValue()));
        assertThat(unQuote("\"Hello World\" to everyone\""), is("Hello World\" to everyone"));
    }

}
