/*
 * Copyright 2015 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package com.intershop.gradle.escrow

import groovy.util.logging.Slf4j
import org.gradle.api.Project

@Slf4j
class EscrowExtension {

    private Project project

    public static final String ESCROW_EXTENSION_NAME = 'escrow'
    public static final String ESCROW_TASK_NAME = 'escrowZip'

    // run on CI server
    public final static String RUNONCI_ENV = 'RUNONCI'
    public final static String RUNONCI_PRJ = 'runOnCI'


    public static final String ESCROW_GROUP_NAME = 'Escrow Build Group'

    final Collection excludesList

    EscrowExtension(Project project) {
        this.project = project

        // init default value for runOnCI
        if(! runOnCI) {
            runOnCI = Boolean.parseBoolean(getVariable(RUNONCI_ENV, RUNONCI_PRJ, 'false'))
            if(runOnCI) {
                log.warn('Escrow task will be executed on a CI build environment for {}.', project.name)
            }
        }

        String buildDirName = project.getBuildDir().getName().toString()

        excludesList = [ buildDirName, "*/${buildDirName}", '.gradle', '.svn', '.git', '.idea', '.eclipse', '.settings', '**/.settings/**']
        sourceGroup = project.getGroup().toString()

        classifier = 'src'
    }

    /**
     * <p>The group configuration for this artifact.</p>
     */
    String sourceGroup

    /**
     * <p>The classifier of this artifact.</p>
     */
    String classifier

    /**
     * <p>Define a single exclude pattern.</p>
     * @param exclude
     */
    void exclude(String exclude) {
        excludesList << exclude
    }

    /**
     * <p>Defines a list of exclude pattern.</p>
     * @param excludes
     */
    void excludes(List<String> excludes) {
        excludesList.addAll(excludes)
    }

    void setExcludes(List<String> excludes) {
        excludesList.clear()
        excludesList.addAll(excludes)
    }

    /**
     * <p>Configuration for the execution on the CI server</p>
     *
     * <p>Can be configured/overwritten with environment variable RUNONCI;
     * java environment RUNONCI or project variable runOnCI</p>
     */
    boolean runOnCI

    /**
     * Calculates the setting for special configuration from the system
     * or java environment or project properties.
     *
     * @param envVar        name of environment variable
     * @param projectVar    name of project variable
     * @param defaultValue  default value
     * @return              the string configuration
     */
    private String getVariable(String envVar, String projectVar, String defaultValue) {
        if(System.properties[envVar]) {
            log.debug('Specified from system property {}.', envVar)
            return System.properties[envVar].toString().trim()
        } else if(System.getenv(envVar)) {
            log.debug('Specified from system environment property {}.', envVar)
            return System.getenv(envVar).toString().trim()
        } else if(project.hasProperty(projectVar) && project."${projectVar}") {
            log.debug('Specified from project property {}.', projectVar)
            return project."${projectVar}".toString().trim()
        }
        return defaultValue
    }
}