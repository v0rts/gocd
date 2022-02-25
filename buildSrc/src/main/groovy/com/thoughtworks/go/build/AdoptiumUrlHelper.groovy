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

package com.thoughtworks.go.build


class AdoptiumUrlHelper {
  static String downloadURL(OperatingSystem operatingSystem, Integer featureVersion, Integer interimVersion, Integer updateVersion, Integer buildVersion) {
    String versionComponent = [featureVersion, interimVersion, updateVersion].findAll({ it != null }).join('.')
    String featureSuffix = updateVersion == null ? '' : 'U'

    "https://github.com/adoptium/temurin${featureVersion}-binaries/releases/download/jdk-${versionComponent}%2B${buildVersion}/OpenJDK${featureVersion}${featureSuffix}-jre_x64_${operatingSystem.adoptiumAlias()}_hotspot_${versionComponent}_${buildVersion}.${operatingSystem.extension}"
  }

  static String sha256sumURL(OperatingSystem operatingSystem, Integer featureVersion, Integer interimVersion, Integer updateVersion, Integer buildVersion) {
    "${downloadURL(operatingSystem, featureVersion, interimVersion, updateVersion, buildVersion)}.sha256.txt"
  }
}
