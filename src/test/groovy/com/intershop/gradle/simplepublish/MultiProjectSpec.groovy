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


package com.intershop.gradle.simplepublish

import com.intershop.gradle.test.util.TestDispatcher
import com.intershop.gradle.test.AbstractIntegrationSpec
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.Rule
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Unroll
class MultiProjectSpec extends AbstractIntegrationSpec {

    @Rule
    public final MockWebServer server = new MockWebServer()

    private static String buildFileContentBase = """
                                          plugins {
                                              id 'java'
                                              id 'ivy-publish'
                                          }
                                          sourceCompatibility = 1.7
                                          targetCompatibility = 1.7

                                          group = 'com.intershop.project'
                                          version = 'VERSION'

                                          if(project.hasProperty("releaseWithJavaDoc") && project.ext.releaseWithJavaDoc.toBoolean()) {
                                            println 'CREATE JAVADOC'
                                          }
                                          publishing {
                                            publications {
                                                ivy(IvyPublication) {
                                                    from components.java
                                                }
                                            }
                                          }
                                          """.stripIndent()

    private static String buildFileContentBaseDouble = """
                                          plugins {
                                              id 'java'
                                              id 'ivy-publish'
                                          }
                                          apply plugin: 'com.intershop.gradle.simplepublish-configuration'

                                          sourceCompatibility = 1.7
                                          targetCompatibility = 1.7

                                          group = 'com.intershop.project'
                                          version = 'VERSION'

                                          if(project.hasProperty("releaseWithJavaDoc") && project.ext.releaseWithJavaDoc.toBoolean()) {
                                            println 'CREATE JAVADOC'
                                          }

                                          publishing {
                                            publications {
                                                ivy(IvyPublication) {
                                                    from components.java
                                                }
                                            }
                                          }
                                          """.stripIndent()

    def 'test release publishing'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.simplepublish-configuration'
            }

            group = 'com.intershop'
            version = '1.0.0'

            publishing {
                repositories {
                    ivy {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()
        createSubProjectJava('project1a', settingsfile, 'com.intereshop.a', buildFileContent, '1.0.0')
        createSubProjectJava('project2b', settingsfile, 'com.intereshop.b', buildFileContent, '1.0.0')

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '--stacktrace', '-i', "-DRUNONCI=true", "-PsnapshotURL=${urlStr}nexus/snapshots", "-PreleaseURL=${urlStr}nexus/releases", '-PrepoUserName=admin', '-PrepoUserPasswd=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('/nexus/releases/')
        }

        then:
        upLoadListCheck

        where:
        buildFileContent << [buildFileContentBase, buildFileContentBaseDouble]
    }

    def 'test snapshot publishing'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.simplepublish-configuration'
            }

            group = 'com.intershop'
            version = '1.0.0-SNAPSHOT'

            publishing {
                repositories {
                    ivy {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()
        createSubProjectJava('project1a', settingsfile, 'com.intereshop.a', buildFileContent, '1.0.0-SNAPSHOT')
        createSubProjectJava('project2b', settingsfile, 'com.intereshop.b', buildFileContent, '1.0.0-SNAPSHOT')

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', "-DRUNONCI=true", "-PsnapshotURL=${urlStr}nexus/snapshots", "-PreleaseURL=${urlStr}nexus/releases", '-PrepoUserName=admin', '-PrepoUserPasswd=admin123', '-s')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('/nexus/snapshots/')
        }

        then:
        ! result.output.contains('CREATE JAVADOC')
        result.task(':project1a:publishIvyPublicationToIntershopIvyCIRepository').outcome == SUCCESS
        result.task(':project2b:publishIvyPublicationToIntershopIvyCIRepository').outcome == SUCCESS
        ! result.tasks.contains(':writeToJira')
        upLoadListCheck

        where:
        buildFileContent << [buildFileContentBase, buildFileContentBaseDouble]
    }

    /**
     * Creates a java sub project
     */
    private File createSubProjectJava(String projectPath, File settingsGradle, String packageName, String buildContent, String version){
        File subProject = createSubProject(projectPath, settingsGradle, buildContent.replace('VERSION', version))
        writeJavaTestClass(packageName, subProject)
        return subProject
    }
}
