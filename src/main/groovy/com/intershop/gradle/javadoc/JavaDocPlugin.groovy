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
package com.intershop.gradle.javadoc

import groovy.transform.CompileDynamic
import groovy.transform.Memoized
import groovy.transform.TypeChecked
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc

import static com.intershop.gradle.util.PluginHelper.*

/**
 * This configuration configure javadoc for java projects.
 * The package is add to the correct publication.
 */
@CompileDynamic
class JavaDocPlugin implements Plugin<Project> {

    private Project project

    void apply(Project project) {
        this.project = project

        project.plugins.withType(JavaPlugin) {
            Javadoc javadocTask = (Javadoc) project.tasks.getByName('javadoc') {Task t ->
                    options {
                        header = '<img src="{@docRoot}/images/intershop_logo.gif">'
                        footer = '<img src="{@docRoot}/images/intershop_logo.gif">'

                        // this plugin supports only JDK 1.8
                        // doclint will be disabled
                        addStringOption('Xdoclint:none', '-quiet')

                        links("http://docs.oracle.com/javase/8/docs/api/")
                    }

                    classpath = project.files(project.configurations.compile).filter {
                        ! it.name.endsWith('-sources.jar') && ! it.name.endsWith('-javadoc.jar')
                    }

                    doLast {
                        project.copy {
                            from logo
                            into(new File(t.destinationDir, 'images'))
                            fileMode = 0666
                        }
                    }
                }

            project.tasks.create('javadocJar', Jar) {
                    dependsOn javadocTask
                    from javadocTask.destinationDir
                    classifier 'javadoc'
                    extension 'jar'
                    group 'build'
                }

            // sources will be only added the the package on the CI server.
            String runOnCI = getVariable(project, RUNONCI_ENV, RUNONCI_PRJ, 'false')



            if (runOnCI.toBoolean()) {
                project.logger.info('Add javadoc package to publishing config: RunOnCI: {}', runOnCI.toBoolean())
                // add javadoc jar for ivy publishing
                project.plugins.withType(IvyPublishPlugin) {
                    project.publishing {
                        publications {
                            ivy(IvyPublication) {
                                configurations {
                                    create("javadoc", { extend "default"})
                                }
                                artifact(project.tasks.javadocJar) {
                                    type 'javadoc'
                                    conf 'javadoc'
                                }
                            }
                        }
                    }
                }

                // add source jar for mvn publishing
                project.plugins.withType(MavenPublishPlugin) {
                    project.publishing {
                        publications {
                            mvn(MavenPublication) {
                                artifact(project.tasks.javadocJar) {
                                    classifier 'javadoc'
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    File getLogo() {
        copyResource('/intershop/javadoc/intershop_logo.gif')
    }

    @TypeChecked
    @Memoized
    private File copyResource(String path) {
        File target = new File(project.buildDir, path)
        target.parentFile.mkdirs()

        target.bytes = getClass().getResourceAsStream(path).bytes

        return target
    }
}
