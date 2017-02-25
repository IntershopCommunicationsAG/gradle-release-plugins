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

package com.intershop.gradle.javasource

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar

import static com.intershop.gradle.util.PluginHelper.*

class JavaSourcePlugin implements Plugin<Project> {

    private Project project

    void apply(Project project) {
        this.project = project

        String runOnCI = getVariable(project, RUNONCI_ENV, RUNONCI_PRJ, 'false')
        project.logger.info('Add javasource package: RunOnCI: {}', runOnCI.toBoolean())

        // sources will be only added the the package on the CI server.
        if (runOnCI.toBoolean()) {
            // create task for sources
            project.plugins.withType(JavaBasePlugin) {
                Task sourceJar = project.tasks.maybeCreate('sourceJar', Jar)
                sourceJar.from(project.sourceSets.main.allJava)
                sourceJar.setClassifier('sources')
            }

            // add source jar for ivy publishing
            project.plugins.withType(IvyPublishPlugin) {
                Task sourceJar = project.tasks.findByName('sourceJar')
                if (sourceJar) {
                    project.publishing {
                        publications {
                            ivy(IvyPublication) {
                                artifact(sourceJar) {
                                    type 'sources'
                                    conf 'runtime'
                                }
                            }
                        }
                    }
                }
            }

            // add source jar for mvn publishing
            project.plugins.withType(MavenPublishPlugin) {
                Task sourceJar = project.tasks.findByName('sourceJar')
                if (sourceJar) {
                    project.publishing {
                        publications {
                            mvn(MavenPublication) {
                                artifact(sourceJar) {
                                    classifier 'sources'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
