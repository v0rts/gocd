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
package com.thoughtworks.go.apiv4.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.Map;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Map.entry;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public class MaterialsRepresenter {
    enum Materials {
        GIT(GitMaterialConfig.class, new GitMaterialRepresenter()),
        HG(HgMaterialConfig.class, new HgMaterialRepresenter()),
        SVN(SvnMaterialConfig.class, new SvnMaterialRepresenter()),
        P4(P4MaterialConfig.class, new P4MaterialRepresenter()),
        TFS(TfsMaterialConfig.class, new TfsMaterialRepresenter());

        private final Class<? extends MaterialConfig> type;
        private final MaterialRepresenter representer;

        Materials(Class<? extends MaterialConfig> type, MaterialRepresenter representer) {
            this.type = type;
            this.representer = representer;
        }

        public static boolean hasMaterial(Class<?> type) {
            return stream(values()).anyMatch(material -> material.type == type);
        }

        public void toJSON(OutputWriter jsonWriter, MaterialConfig materialConfig) {
            jsonWriter.add("type", this.name().toLowerCase());
            jsonWriter.addChild("attributes", attributeWriter -> {
                this.representer.toJSON(attributeWriter, materialConfig);
                addErrors(attributeWriter, materialConfig);
            });
        }
    }

    private static final Map<String, String> ERROR_MAPPING = Map.ofEntries(
        entry("materialName", "name"),
        entry("folder", "destination"),
        entry("autoUpdate", "auto_update"),
        entry("filterAsString", "filter"),
        entry("checkexternals", "check_externals"),
        entry("serverAndPort", "port"),
        entry("useTickets", "use_tickets"),
        entry("pipelineName", "pipeline"),
        entry("stageName", "stage"),
        entry("pipelineStageName", "pipeline"),
        entry("packageId", "ref"),
        entry("scmId", "ref"),
        entry("encryptedPassword", "encrypted_password")
    );

    public static void toJSON(OutputWriter jsonWriter, MaterialConfig materialConfig) {
        validateSupportedMaterials(materialConfig);
        stream(Materials.values())
            .filter(material -> material.type == materialConfig.getClass())
            .findFirst()
            .ifPresent(material -> material.toJSON(jsonWriter, materialConfig));
    }

    static MaterialConfig fromJSON(JsonReader jsonReader) {
        String type = jsonReader.getString("type");
        JsonReader attributes = jsonReader.readJsonObject("attributes");

        return stream(Materials.values())
            .filter(material -> equalsIgnoreCase(type, material.name()))
            .findFirst()
            .map(material -> material.representer.fromJSON(attributes))
            .orElseThrow(unprocessableMaterialType(type));
    }

    private static void addErrors(OutputWriter json, MaterialConfig material) {
        if (!material.errors().isEmpty()) {
            json.addChild("errors", errorWriter -> new ErrorGetter(ERROR_MAPPING).toJSON(errorWriter, material));
        }
    }

    private static Supplier<UnprocessableEntityException> unprocessableMaterialType(String type) {
        return () -> new UnprocessableEntityException(String.format("Unsupported material type: %s. It has to be one of 'git, hg, svn, p4 and tfs'.", type));
    }

    private static void validateSupportedMaterials(MaterialConfig material) {
        if (!Materials.hasMaterial(material.getClass())) {
            throw new IllegalArgumentException(format("Cannot serialize unsupported material type: %s", material.getClass().getSimpleName()));
        }
    }
}
