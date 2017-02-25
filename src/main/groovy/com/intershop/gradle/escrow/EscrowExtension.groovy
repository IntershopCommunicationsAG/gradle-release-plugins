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

import static com.intershop.gradle.util.PluginHelper.*

@Slf4j
class EscrowExtension {

    private Project project

    public static final String ESCROW_EXTENSION_NAME = 'escrow'
    public static final String ESCROW_TASK_NAME = 'escrowZip'

    public static final String ESCROW_GROUP_NAME = 'Escrow Build Group'

    final Collection excludesList

    EscrowExtension(Project project) {
        this.project = project

        // init default value for runOnCI
        if(! runOnCI) {
            runOnCI = Boolean.parseBoolean(getVariable(project, RUNONCI_ENV, RUNONCI_PRJ, 'false'))
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
}