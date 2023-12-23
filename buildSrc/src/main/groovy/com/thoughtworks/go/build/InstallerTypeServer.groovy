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

package com.thoughtworks.go.build

class InstallerTypeServer implements InstallerType {
  @Override
  String getBaseName() {
    'go-server'
  }

  @Override
  String getJarFileName() {
    'go.jar'
  }

  @Override
  String getLogFileName() {
    'go-server-wrapper.log'
  }

  @Override
  Map<String, String> getAdditionalEnvVars() {
    [:]
  }

  @Override
  Map<String, String> getAdditionalLinuxEnvVars() {
    [:]
  }

  @Override
  List<String> getJvmModuleOpensArgs() {
    [
      '--add-opens=java.base/java.lang=ALL-UNNAMED', // Required for Hibernate 3.6/Javassist proxying (at minimum, may be used for other things)
      '--add-opens=java.base/java.util=ALL-UNNAMED', // Required at least for cloning GoConfig subclasses of java.util classes :(
    ]
  }

  @Override
  List<String> getJvmArgs() {
    getJvmModuleOpensArgs() + [
      '-Xms512m',
      '-Xmx1024m',
      '-XX:MaxMetaspaceSize=400m',
      '-Duser.language=en',
      '-Duser.country=US',
    ]
  }

  @Override
  List<String> getLinuxJvmArgs() {
    [
      '-Dgocd.server.log.dir=/var/log/go-server',
      '-Dcruise.config.dir=/etc/go',
      '-Dcruise.config.file=/etc/go/cruise-config.xml',
    ]
  }

  @Override
  boolean getAllowPassthrough() {
    false
  }

  @Override
  Map<String, Object> getDirectories() {
    [
      '/usr/share/doc/go-server'           : [mode: 0755, owner: 'root', group: 'root', ownedByPackage: true],
      '/usr/share/go-server/wrapper-config': [mode: 0750, owner: 'root', group: 'go', ownedByPackage: true],
      '/var/lib/go-server'                 : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true],
      '/var/lib/go-server/run'             : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true],
      '/var/log/go-server'                 : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true],
      '/var/run/go-server'                 : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true],
      '/etc/go'                            : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true],
    ]
  }

  @Override
  Map<String, Object> getConfigFiles() {
    [
      '/usr/share/go-server/wrapper-config/wrapper.conf'           : [mode: 0640, owner: 'root', group: 'go', ownedByPackage: true, confFile: true],
      '/usr/share/go-server/wrapper-config/wrapper-properties.conf': [mode: 0640, owner: 'root', group: 'go', ownedByPackage: true, confFile: true],
    ]
  }

  @Override
  String getPackageDescription() {
    '''
    GoCD Server
    Component
    Next generation
    continuous integration and release management server from Thoughtworks.
    '''.stripIndent().trim()
  }

  @Override
  String getWindowsAndOSXServiceName() {
    return 'Go Server'
  }

}
