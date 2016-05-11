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
package com.intershop.gradle.nexuspublish

import com.intershop.gradle.test.AbstractIntegrationSpec
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.Rule
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Unroll
class MultiProjectSpec extends AbstractIntegrationSpec {

    static String issueKey = 'ISTOOLS-993'

    String configURL = System.properties['configURL']
    String configToken = System.properties['configURLToken']

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
                                          apply plugin: 'com.intershop.gradle.nexuspublish-configuration'

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

    @Rule
    public final MockWebServer server = new MockWebServer()

    def 'test release publishing with staging'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.scmversion' version '1.0.4'
                id 'com.intershop.gradle.nexuspublish-configuration'
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

        File changelog = file('build/changelog/changelog.asciidoc')
        changelog << """
        = Change Log for 2.0.0

        This list contains changes since version 1.0.0. +
        Created: Sun Feb 21 17:11:48 CET 2016

        [cols="5%,5%,90%", width="95%", options="header"]
        |===
        3+| ${issueKey} change on master (e6c62c43)
        | | M |  gradle.properties
        3+| remove unnecessary files (a2da48ad)
        | | D | gradle/wrapper/gradle-wrapper.jar
        | | D | gradle/wrapper/gradle-wrapper.properties
        |===""".stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DNEXUSBASEURL=${urlStr}nexus/", '-DNEXUSUSERNAME=admin', '-DNEXUSUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('/nexus/service/local/staging/deploy/maven2/')
        }

        then:
        result.output.contains('CREATE JAVADOC')
        result.task(':upload').outcome == SUCCESS
        result.task(':setIssueField').outcome == SUCCESS
        result.task(':project1a:publishIvyPublicationToIvyNexusStagingRepository').outcome == SUCCESS
        result.task(':project2b:publishIvyPublicationToIvyNexusStagingRepository').outcome == SUCCESS
        upLoadListCheck
        responses.get('onebody').contains('"project":{"key":"ISTOOLS"}')
        responses.get('onebody').contains('"issuetype":{"id":"10001"}')

        where:
        buildFileContent << [buildFileContentBase, buildFileContentBaseDouble]
    }

    def 'test release publishing without staging'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.scmversion' version '1.0.4'
                id 'com.intershop.gradle.nexuspublish-configuration'
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

        File changelog = file('project1a/build/changelog/changelog.asciidoc')
        changelog << """
        = Change Log for 2.0.0

        This list contains changes since version 1.0.0. +
        Created: Sun Feb 21 17:11:48 CET 2016

        [cols="5%,5%,90%", width="95%", options="header"]
        |===
        3+| ${issueKey} change on master (e6c62c43)
        | | M |  gradle.properties
        3+| remove unnecessary files (a2da48ad)
        | | D | gradle/wrapper/gradle-wrapper.jar
        | | D | gradle/wrapper/gradle-wrapper.properties
        |===""".stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DNEXUSBASEURL=${urlStr}nexus/", '-DNEXUSUSERNAME=admin', '-DNEXUSUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('/nexus/content/repositories/snapshots/')
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
