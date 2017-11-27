/*
 * Copyright 2017 Intershop Communications AG.
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

import groovy.transform.CompileDynamic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Zip

/**
 * This is the implementation of the plugin.
 */
@CompileDynamic
class EscrowPlugin implements Plugin<Project> {

    private EscrowExtension pluginExtension

    void apply(Project project) {
        project.logger.info('Create extension {} for {}', EscrowExtension.ESCROW_EXTENSION_NAME, project.name)
        pluginExtension = project.extensions.create(EscrowExtension.ESCROW_EXTENSION_NAME, EscrowExtension, project)

        if (pluginExtension.isRunOnCI() && pluginExtension.runOnCI) {
            project.plugins.withType(IvyPublishPlugin) {
                project.publishing {
                    if(! project.getVersion().toString().toLowerCase().endsWith('snapshot')) {
                        publications {
                            ivy(IvyPublication) {
                                organisation = pluginExtension.getSourceGroup()
                                artifact(getConfigurePackageTask(project)) {
                                    classifier = pluginExtension.getClassifier()
                                }
                            }
                        }
                    }
                }
            }

            project.plugins.withType(MavenPublishPlugin) {
                project.publishing {
                    if(! project.getVersion().toString().toLowerCase().endsWith('snapshot')) {
                        publications {
                            mvn(MavenPublication) {
                                groupId = pluginExtension.getSourceGroup()
                                artifact(getConfigurePackageTask(project)) {
                                    classifier = pluginExtension.getClassifier()
                                }
                            }
                        }
                    }
                }
            }
        }

        if(project.getName() != project.getRootProject().getName()) {
            throw new GradleException('It is not possible to apply this "escrow" plugin to Gradle sub projects.')
        }
    }

    private Task getConfigurePackageTask(Project project) {
        def task = project.tasks.create(EscrowExtension.ESCROW_TASK_NAME, Zip.class).configure {
            description = "Creates an escrow source package from project"
            group = EscrowExtension.ESCROW_GROUP_NAME
            baseName = project.rootProject.getName()

            includeEmptyDirs = true
            zip64 = true

            from project.rootProject.rootDir
            into project.rootProject.name

            includeEmptyDirs = true
            setExcludes(pluginExtension.getExcludesList())

            destinationDir = new File(project.getBuildDir(), EscrowExtension.ESCROW_EXTENSION_NAME)
        }

        return task
    }
}
