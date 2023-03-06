/*
 * Copyright 2023 Thoughtworks, Inc.
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

package com.thoughtworks.go.build.docker

import com.thoughtworks.go.build.Architecture
import freemarker.cache.ClassTemplateLoader
import freemarker.core.PlainTextOutputFormat
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

enum ImageType {
  server,
  agent
}

class BuildDockerImageTask extends DefaultTask {
  @Input Distro distro
  @Input DistroVersion distroVersion
  @Input String tiniVersion
  @InputFile File artifactZip
  @Input ImageType imageType
  // Not really a classic output dir from Gradle perspective, as multiple tasks share dir from parent with unique tarballs per distribution.
  // We use a string for Gradle 7 compatibility, and because we don't want Gradle to consider the actual contents.
  @Input String outputDir
  @Internal Closure templateHelper
  @Internal Closure verifyHelper

  BuildDockerImageTask() {
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }
  }

  @TaskAction
  def perform() {
    if (distroVersion.pastEolGracePeriod) {
      throw new RuntimeException("The image $distro:v$distroVersion.version is unsupported. EOL was ${distroVersion.eolDate}, and GoCD build grace period has passed.")
    }

    if (distroVersion.eol && !distroVersion.continueToBuild) {
      throw new RuntimeException("The image $distro:v$distroVersion.version was EOL on ${distroVersion.eolDate}. Set :continueToBuild option to continue building through the grace period.")
    }

    if (distroVersion.aboutToEol) {
      println("WARNING: The image $distro:v$distroVersion.version is supposed to be EOL on ${distroVersion.eolDate}. Derived GoCD image will be marked as deprecated.")
    }

    project.delete(gitRepoDirectory)
    def credentials = "${System.getenv("GIT_USER")}:${System.getenv("GIT_PASSWORD")}"
    project.exec {
      workingDir = project.rootProject.projectDir
      commandLine = ["git", "clone", "--depth=1", "--quiet", "https://${credentials}@github.com/gocd/${gitHubRepoName}", gitRepoDirectory]
    }

    if (templateHelper != null) {
      templateHelper.call()
    }

    project.copy {
      from artifactZip
      into gitRepoDirectory
    }

    writeTemplateToFile(templateFile(), dockerfile)

    if (!project.hasProperty('skipDockerBuild')) {
      logger.lifecycle("Building ${distro} image for ${distro.supportedArchitectures}. (Current build architecture is ${Architecture.current()}).")

      // build image
      project.mkdir(imageTarFile.parentFile)
      executeInGitRepo("docker", "buildx", "build",
        "--pull",
        "--platform", supportedPlatforms.join(","),
        "--output", "type=oci,dest=${imageTarFile}",
        ".",
        "--tag", imageNameWithTag
      )

      // verify image
      def isNativeVerify = distro.dockerVerifyArchitecture == Architecture.current()
      if (verifyHelper != null && (isNativeVerify || !project.hasProperty('dockerBuildSkipNonNativeVerify'))) {
        // Load image  into local docker from buildx for sanity checking
        executeInGitRepo("docker", "buildx", "build",
          "--quiet",
          "--load",
          "--platform", "linux/${distro.dockerVerifyArchitecture.dockerAlias}",
          ".",
          "--tag", imageNameWithTag
        )

        logger.lifecycle("\nVerifying ${imageNameWithTag} image for ${distro.dockerVerifyArchitecture}. (Current build architecture is ${Architecture.current()}).\n")
        verifyHelper.call(isNativeVerify)
        logger.lifecycle("\nVerification of ${imageNameWithTag} image on ${distro.dockerVerifyArchitecture} successful.")
      }
    }

    logger.lifecycle("Cleaning up...")
    // delete the image, to save space
    if (!project.hasProperty('dockerBuildKeepImages')) {
      project.exec {
        workingDir = project.rootProject.projectDir
        commandLine = ["docker", "rmi", imageNameWithTag]
      }
    }

    project.delete("${gitRepoDirectory}/${artifactZip.name}")

    if (project.hasProperty('dockerGitPush') && project.dockerGitPush == 'I_REALLY_WANT_TO_DO_THIS') {
      executeInGitRepo("git", "add", ".")
      executeInGitRepo("git", "commit", "-m", "Bump to version ${project.fullVersion}", "--author", "GoCD CI User <godev+gocd-ci-user@thoughtworks.com>")
      executeInGitRepo("git", "tag", "v${project.goVersion}")
      executeInGitRepo("git", "push")
      executeInGitRepo("git", "push", "--tags")
    }
  }

  def executeInGitRepo(Object... args) {
    project.exec {
      workingDir = gitRepoDirectory
      commandLine = args
    }
  }

  @Internal
  GString getImageNameWithTag() {
    "${dockerImageName}:${imageTag}"
  }

  @Internal
  Set<GString> getSupportedPlatforms() {
    distro.supportedArchitectures.collect {"linux/${it.dockerAlias}" }
  }

  @Input
  GString getImageTag() {
    "v${project.fullVersion}"
  }

  @Internal
  File getImageTarFile() {
    project.file("${outputDir}/gocd-${imageType.name()}-${dockerImageName}-v${project.fullVersion}.tar")
  }

  void writeTemplateToFile(String templateFile, File outputFile) {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_28)
    configuration.setDefaultEncoding("utf-8")
    configuration.setLogTemplateExceptions(true)
    configuration.setNumberFormat("computer")
    configuration.setOutputFormat(PlainTextOutputFormat.INSTANCE)
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
    configuration.setTemplateLoader(new ClassTemplateLoader(BuildDockerImageTask.classLoader, "/gocd-docker-${imageType.name()}"))

    Template template = configuration.getTemplate(templateFile, "utf-8")

    def templateVars = [
      distro                         : distro,
      distroVersion                  : distroVersion,
      project                        : project,
      goVersion                      : project.goVersion,
      fullVersion                    : project.fullVersion,
      gitRevision                    : project.gitRevision,
      additionalFiles                : additionalFiles,
      imageName                      : dockerImageName,
      copyrightYear                  : project.copyrightYear,
      useFromArtifact                : !project.hasProperty('dockerGitPush'),
      dockerAliasToWrapperArchAsShell: Architecture.dockerAliasToWrapperArchAsShell(),
    ]

    project.mkdir(project.buildDir)

    outputFile.withWriter("utf-8") { writer ->
      template.process(templateVars, writer)
    }
  }

  private GString templateFile() {
    "Dockerfile.${imageType.name()}.ftl"
  }

  @Internal
  File getGitRepoDirectory() {
    project.file("${project.buildDir}/${gitHubRepoName}")
  }

  @Internal
  String getGitHubRepoName() {
    if (imageType == ImageType.agent) {
      if (distro == Distro.docker) {
        return "docker-gocd-agent-dind"
      } else {
        return "docker-gocd-agent-${distro.name()}-${distroVersion.version}"
      }
    }
    if (distro == Distro.alpine) {
      return "docker-gocd-server"
    } else {
      return "docker-gocd-server-${distro.name()}-${distroVersion.version}"
    }
  }

  @Internal
  String getDockerImageName() {
    if (imageType == ImageType.agent) {
      if (distro == Distro.docker) {
        return "gocd-agent-${distro.name()}-dind"
      } else {
        return "gocd-agent-${distro.name()}-${distroVersion.version}"
      }
    }
    if (distro == Distro.alpine) {
      return "gocd-server"
    } else {
      return "gocd-server-${distro.name()}-${distroVersion.version}"
    }
  }

  @Internal
  protected File getDockerfile() {
    project.file("${gitRepoDirectory}/Dockerfile")
  }

  @Internal
  Map<String, Map<String, String>> getAdditionalFiles() {
    return [
      '/usr/local/sbin/tini': [
        url  : "https://github.com/krallin/tini/releases/download/v${tiniVersion}/tini-static-\${TARGETARCH}".toString(),
        mode : '0755',
        owner: 'root',
        group: 'root'
      ]
    ]
  }
}
