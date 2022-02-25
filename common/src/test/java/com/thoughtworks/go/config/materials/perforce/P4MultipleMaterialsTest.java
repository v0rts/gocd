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
package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.perforce.P4Client;
import com.thoughtworks.go.domain.materials.perforce.P4Fixture;
import com.thoughtworks.go.helper.P4TestRepo;
import com.thoughtworks.go.util.TempDirUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.thoughtworks.go.config.MaterialRevisionsMatchers.containsModifiedFile;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class P4MultipleMaterialsTest {
    public static final String DEFAULT_CLIENT_NAME = "p4test_2";

    protected P4Client p4;
    protected File clientFolder;
    protected P4Fixture p4Fixture;

    private static final String VIEW_SRC = "//depot/src/... //something/...";
    private static final String VIEW_LIB = "//depot/lib/... //something/...";

    P4TestRepo p4TestRepo;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        p4Fixture = new P4Fixture();
        clientFolder = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();
        p4TestRepo = createTestRepo(tempDir);
        p4Fixture.setRepo(p4TestRepo);
        p4 = P4Client.fromServerAndPort(null, p4Fixture.port(), "cceuser", null, DEFAULT_CLIENT_NAME, false, clientFolder, "", inMemoryConsumer(), true);
        p4TestRepo.onSetup();
        p4Fixture.start();
    }


    @AfterEach
    public void stopP4Server() {
        try {
            FileUtils.deleteDirectory(clientFolder);
            p4Fixture.stop(p4);
        } catch (IOException ignore) {

        }
    }

    protected P4TestRepo createTestRepo(Path tempDir) throws Exception {
        P4TestRepo repo = P4TestRepo.createP4TestRepo(tempDir, clientFolder);
        repo.onSetup();
        return repo;
    }

    @Test
    public void shouldUpdateToItsDestFolder() {
        P4Material p4Material = p4Fixture.material(VIEW_SRC, "dest1");

        MaterialRevision revision = new MaterialRevision(p4Material, p4Material.latestModification(clientFolder, new TestSubprocessExecutionContext()));

        revision.updateTo(clientFolder, inMemoryConsumer(), new TestSubprocessExecutionContext());

        assertThat(new File(clientFolder, "dest1/net").exists(), is(true));
    }

    @Test
    public void shouldIgnoreDestinationFolderWhenUpdateToOnServerSide() {
        P4Material p4Material = p4Fixture.material(VIEW_SRC, "dest1");

        MaterialRevision revision = new MaterialRevision(p4Material, p4Material.latestModification(clientFolder, new TestSubprocessExecutionContext()));

        revision.updateTo(clientFolder, inMemoryConsumer(), new TestSubprocessExecutionContext(true));

        assertThat(new File(clientFolder, "dest1/net").exists(), is(false));
        assertThat(new File(clientFolder, "net").exists(), is(true));
    }

    @Test
    public void shouldFoundModificationsForEachMaterial() throws Exception {
        P4Material p4Material1 = p4Fixture.material(VIEW_SRC, "src");
        P4Material p4Material2 = p4Fixture.material(VIEW_LIB, "lib");
        Materials materials = new Materials(p4Material1, p4Material2);

        p4TestRepo.checkInOneFile(p4Material1, "filename.txt");
        p4TestRepo.checkInOneFile(p4Material2, "filename2.txt");

        MaterialRevisions materialRevisions = materials.latestModification(clientFolder, new TestSubprocessExecutionContext());

        assertThat(materialRevisions.getRevisions().size(), is(2));
        assertThat(materialRevisions, containsModifiedFile("src/filename.txt"));
        assertThat(materialRevisions, containsModifiedFile("lib/filename2.txt"));
    }
}
