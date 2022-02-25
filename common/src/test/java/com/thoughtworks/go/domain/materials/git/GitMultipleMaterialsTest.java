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
package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.thoughtworks.go.config.MaterialRevisionsMatchers.containsModifiedFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GitMultipleMaterialsTest {
    private GitTestRepo repo;
    private File pipelineDir;

    @BeforeEach
    public void createRepo(@TempDir Path tempDir) throws IOException {
        repo = new GitTestRepo(tempDir);
        pipelineDir = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();
    }

    @Test
    public void shouldCloneMaterialToItsDestFolder() throws Exception {
        GitMaterial material1 = repo.createMaterial("dest1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));

        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext());

        assertThat(new File(pipelineDir, "dest1").exists(), is(true));
        assertThat(new File(pipelineDir, "dest1/.git").exists(), is(true));
    }

    @Test
    public void shouldIgnoreDestinationFolderWhenCloningMaterialWhenServerSide() throws Exception {
        GitMaterial material1 = repo.createMaterial("dest1");

        MaterialRevision materialRevision = new MaterialRevision(material1, material1.latestModification(pipelineDir, new TestSubprocessExecutionContext()));

        materialRevision.updateTo(pipelineDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext(true));

        assertThat(new File(pipelineDir, "dest1").exists(), is(false));
        assertThat(new File(pipelineDir, ".git").exists(), is(true));
    }

    @Test
    public void shouldFindModificationsForBothMaterials() throws Exception {
        Materials materials = new Materials(repo.createMaterial("dest1"), repo.createMaterial("dest2"));

        String fileName = "newFile.txt";
        repo.addFileAndPush(fileName, "add a new file " + fileName);

        MaterialRevisions materialRevisions = materials.latestModification(pipelineDir, new TestSubprocessExecutionContext());

        assertThat(materialRevisions.getRevisions().size(), is(2));
        assertThat(materialRevisions, containsModifiedFile(fileName));
    }
}