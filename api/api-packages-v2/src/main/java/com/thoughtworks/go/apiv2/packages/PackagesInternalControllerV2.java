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
package com.thoughtworks.go.apiv2.packages;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.CrudController;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv2.packages.representers.PackageDefinitionRepresenter;
import com.thoughtworks.go.apiv2.packages.representers.VerifyConnectionRepresenter;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.materials.PackageDefinitionService;
import com.thoughtworks.go.server.service.materials.PackageRepositoryService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.function.Consumer;

import static spark.Spark.*;

@Component
public class PackagesInternalControllerV2 extends ApiController implements SparkSpringController, CrudController<PackageDefinition> {

    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private final EntityHashingService entityHashingService;
    private final PackageDefinitionService packageDefinitionService;
    private PackageRepositoryService packageRepositoryService;

    @Autowired
    public PackagesInternalControllerV2(ApiAuthenticationHelper apiAuthenticationHelper,
                                        EntityHashingService entityHashingService,
                                        PackageDefinitionService packageDefinitionService,
                                        PackageRepositoryService packageRepositoryService) {
        super(ApiVersion.v2);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.entityHashingService = entityHashingService;
        this.packageDefinitionService = packageDefinitionService;
        this.packageRepositoryService = packageRepositoryService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.Packages.INTERNAL_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(Routes.Packages.VERIFY_CONNECTION, mimeType, this::setContentType);
            before(Routes.Packages.VERIFY_CONNECTION, mimeType, this.apiAuthenticationHelper::checkAdminUserOrGroupAdminUserAnd403);

            post(Routes.Packages.VERIFY_CONNECTION, mimeType, this::verifyConnection);
        });
    }

    public String verifyConnection(Request request, Response response) throws IOException {
        PackageDefinition packageDefinition = buildEntityFromRequestBody(request);
        packageDefinition.setRepository(packageRepositoryService.getPackageRepository(packageDefinition.getRepository().getId()));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        packageDefinitionService.checkConnection(packageDefinition, result);
        response.status(result.httpCode());
        return writerForTopLevelObject(request, response, writer -> VerifyConnectionRepresenter.toJSON(writer, result, packageDefinition));
    }

    @Override
    public String etagFor(PackageDefinition entityFromServer) {
        return entityHashingService.hashForEntity(entityFromServer);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PackageDefinition;
    }

    @Override
    public PackageDefinition doFetchEntityFromConfig(String packageId) {
        return packageDefinitionService.find(packageId);
    }

    @Override
    public PackageDefinition buildEntityFromRequestBody(Request req) {
        JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(req.body());
        return PackageDefinitionRepresenter.fromJSON(jsonReader);
    }

    @Override
    public Consumer<OutputWriter> jsonWriter(PackageDefinition packages) {
        return outputWriter -> PackageDefinitionRepresenter.toJSON(outputWriter, packages);
    }
}
