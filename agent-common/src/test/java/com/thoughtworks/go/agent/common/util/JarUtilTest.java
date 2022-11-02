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
package com.thoughtworks.go.agent.common.util;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thoughtworks.go.agent.testhelper.FakeGoServer.TestResource.TEST_AGENT;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

public class JarUtilTest {

    private static final String PATH_WITH_HASHES = "#hashes#in#path/";

    @TempDir
    public File temporaryFolder;

    @BeforeEach
    public void setUp() throws IOException {
        TEST_AGENT.copyTo(new File(PATH_WITH_HASHES + "test-agent.jar"));
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteQuietly(new File(PATH_WITH_HASHES + "test-agent.jar"));
        FileUtils.deleteDirectory(new File(PATH_WITH_HASHES));
    }

    @Test
    public void shouldGetManifestKey() throws Exception {
        String manifestKey = JarUtil.getManifestKey(new File(PATH_WITH_HASHES + "test-agent.jar"), "Go-Agent-Bootstrap-Class");
        assertThat(manifestKey, is("com.thoughtworks.go.HelloWorldStreamWriter"));
    }

    @Test
    public void shouldExtractJars() throws Exception {
        File sourceFile = new File(PATH_WITH_HASHES + "test-agent.jar");
        Set<File> files = new HashSet<>(JarUtil.extractFilesInLibDirAndReturnFiles(sourceFile, jarEntry -> jarEntry.getName().endsWith(".class"), temporaryFolder));

        Set<File> actualFiles = Files.list(temporaryFolder.toPath()).map(Path::toFile).collect(Collectors.toSet());

        assertEquals(files, actualFiles);
        assertEquals(files.size(), 2);
        Set<String> fileNames = files.stream().map(File::getName).collect(Collectors.toSet());
        assertEquals(fileNames, new HashSet<>(Arrays.asList("ArgPrintingMain.class", "HelloWorldStreamWriter.class")));
    }
}
