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
package com.thoughtworks.go.server.service.materials.commands;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PackageDefinitionCreatorTest {
    PackageDefinitionService packageDefinitionService;
    CruiseConfig cruiseConfig;
    PackageRepository packageRepository;
    PackageDefinition packageDefinition;

    String repoId = "repo-id";
    String pkgId = "pkg-id";
    String pkgName = "pkg-name";

    @BeforeEach
    public void setup() {
        packageDefinitionService = mock(PackageDefinitionService.class);
        doNothing().when(packageDefinitionService).performPluginValidationsFor(any(PackageDefinition.class));

        cruiseConfig = mock(BasicCruiseConfig.class);
        packageRepository = PackageRepositoryMother.create(repoId);
        packageDefinition = PackageDefinitionMother.create(pkgId);
        packageRepository.addPackage(packageDefinition);
        when(cruiseConfig.getPackageRepositories()).thenReturn(new PackageRepositories(packageRepository));
        doNothing().when(cruiseConfig).savePackageDefinition(any(PackageDefinition.class));
    }

    @Test
    public void testCreateNewPackageDefinition() throws Exception {
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialCreation(repoId, pkgName);

        PackageDefinitionCreator packageDefinitionCreator = new PackageDefinitionCreator(packageDefinitionService, params);
        PackageDefinition newPackageDefinition = packageDefinitionCreator.createNewPackageDefinition(cruiseConfig);

        assertThat(newPackageDefinition.getName(), is(pkgName));
        assertThat(newPackageDefinition.getRepository(), is(packageRepository));

        verify(packageDefinitionService).performPluginValidationsFor(any(PackageDefinition.class));
        verify(cruiseConfig).savePackageDefinition(any(PackageDefinition.class));
    }

    @Test
    public void testGetPackageDefinition() throws Exception {
        HashMap<String, Serializable> params = PackageDefinitionMother.paramsForPackageMaterialAssociation(repoId, pkgId);
        PackageDefinitionCreator packageDefinitionCreator = new PackageDefinitionCreator(packageDefinitionService, params);
        PackageDefinition fetchedPackageDefinition = packageDefinitionCreator.getPackageDefinition(cruiseConfig);

        assertThat(fetchedPackageDefinition.getId(), is(pkgId));
    }
}
