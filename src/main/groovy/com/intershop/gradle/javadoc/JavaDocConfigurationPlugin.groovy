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
package com.intershop.gradle.javadoc

import groovy.transform.Memoized
import groovy.transform.TypeChecked
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
/**
 * This is the implementation of the plugin.
 */
class JavaDocConfigurationPlugin implements Plugin<Project> {

    private Project project

    void apply(Project project) {
        this.project = project

        if(! project.tasks.findByName('copyJavaDocStylesheet')) {
            project.tasks.create('copyJavaDocStylesheet') {
                outputs.file new File(project.getBuildDir(), 'javadoctmp/intershop.css')

                doLast {
                    project.copy {
                        from getStyleSheet()
                        into new File(project.getBuildDir(), 'javadoctmp')
                        fileMode = 0666
                    }
                }
            }
        }

        project.afterEvaluate {
        project.tasks.withType(Javadoc) { task ->
            task.dependsOn project.tasks.copyJavaDocStylesheet
            task.options {
                header = '<img src="{@docRoot}/images/intershop_logo.gif">'
                footer = '<img src="{@docRoot}/images/intershop_logo.gif">'

                stylesheetFile = new File(project.getBuildDir(), 'javadoctmp/intershop.css')

                def javaVersion = System.getProperty('java.version')
                if(javaVersion.startsWith('1.8')) {
                    // This will disable doclint. doclint comes with JDK 1.8.
                    // doclint requires valid HTML 4.01 in JavaDoc
                    // invalid Javadoc will break the build!
                    addStringOption('Xdoclint:none', '-quiet')
                }

                links("http://docs.oracle.com/javase/8/docs/api/")
            }

            task.doLast {
                project.copy {
                    from logo
                    into(new File(task.destinationDir, 'images'))
                    fileMode = 0666
                }
            }
            }
        }
    }

    File getStyleSheet() {
        copyResource('/intershop/javadoc/intershop.css')
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
