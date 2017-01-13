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


package com.intershop.gradle.artifactorypublish

import com.intershop.gradle.test.AbstractIntegrationSpec
import com.intershop.gradle.test.util.TestDispatcher
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.Rule
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Unroll
class MultiProjectSimpleArtifactoryPublishSpec extends AbstractIntegrationSpec {

    String configURL = System.properties['configURL']
    String configToken = System.properties['configURLToken']

    private static String buildFileContentBase = """
                                          plugins {
                                                id 'java'
                                                id 'ivy-publish'
                                            }

                                            publishing {
                                                publications {
                                                    ivy(IvyPublication) {
                                                        from components.java
                                                    }
                                                }
                                            }
                                          """.stripIndent()

    private static String mavenBuildFileContentBase = """
                                          plugins {
                                                id 'java'
                                                id 'maven-publish'
                                            }
                                            
                                            publishing {
                                                publications {
                                                    maven(MavenPublication) {
                                                        from components.java
                                                    }
                                                }
                                            }
                                          """.stripIndent()

    @Rule
    public final MockWebServer server = new MockWebServer()

    @Rule
    public final MockWebServer artifactoryServer = new MockWebServer()

    def 'test release publishing with artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
                plugins {
                  id "ivy-publish"
                  id 'com.intershop.gradle.scmversion' version '1.3.0'
                  id 'com.intershop.gradle.buildinfo' version '2.0.0'
                  id 'com.intershop.gradle.simpleartifactorypublish-configuration'
                }

                group = 'com.intershop.testproject'
                version = '1.0.0'

                artifactory {
                    publish {
                        repository {
                            maven = false
                        }
                        defaults {
                            publications('ivy')
                            properties = ['testBla': 'testBla']
                        }
                    }
                }

                subprojects {
                    group = 'com.intershop.testproject'
                    version = rootProject.getVersion()
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
                .withArguments('artifactoryPublish', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com.intershop.testproject/')
        }

        then:
        result.task(':artifactoryPublish').outcome == SUCCESS
        upLoadListCheck
        upLoadList.size() == 4

        where:
        buildFileContent << [buildFileContentBase]
    }

    def 'test release publishing with artifactory and maven'() {
        given:
        String artifactoryStr = artifactoryServer.url('/').toString()

        List<String> upLoadList = []
        Map<String,String> responses = [:]

        artifactoryServer.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
                plugins {
                  id "maven-publish"
                  id 'com.intershop.gradle.scmversion' version '1.3.0'
                  id 'com.intershop.gradle.buildinfo' version '2.0.0'
                  id 'com.intershop.gradle.simpleartifactorypublish-configuration'
                }

                group = 'com.intershop.testproject'
                version = '1.0.0'

                artifactory {
                    publish {
                        defaults {
                            publications('maven')
                        }
                    }
                }

                subprojects {
                    group = 'com.intershop.testproject'
                    version = rootProject.getVersion()
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
                .withArguments('artifactoryPublish', '--stacktrace', '-i', "-DRUNONCI=true", "-DARTIFACTORYBASEURL=${artifactoryStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com/intershop/testproject/')
        }

        then:
        result.task(':artifactoryPublish').outcome == SUCCESS
        upLoadListCheck
        upLoadList.size() == 4

        where:
        buildFileContent << [mavenBuildFileContentBase]
    }

    def 'test publishing without artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
                plugins {
                  id "ivy-publish"
                  id 'com.intershop.gradle.scmversion' version '1.3.0'
                  id 'com.intershop.gradle.buildinfo' version '2.0.0'
                  id 'com.intershop.gradle.simpleartifactorypublish-configuration'
                }

                group = 'com.intershop.testproject'
                version = '1.0.0'

                artifactory {
                    publish {
                        repository {
                            maven = false
                        }
                        defaults {
                            publications('ivy')
                        }
                    }
                }

                subprojects {
                    group = 'com.intershop.testproject'
                    version = rootProject.getVersion()
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
                .withArguments('publish', '--stacktrace', '-i')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com.intershop.testproject/')
        }

        then:
        ! result.tasks.contains(':artifactoryPublish')
        upLoadListCheck
        upLoadList.size() == 0

        where:
        buildFileContent << [buildFileContentBase]
    }

    def 'test snapshot publishing with artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
                plugins {
                  id "ivy-publish"
                  id 'com.intershop.gradle.scmversion' version '1.3.0'
                  id 'com.intershop.gradle.buildinfo' version '2.0.0'
                  id 'com.intershop.gradle.simpleartifactorypublish-configuration'
                }

                group = 'com.intershop.testproject'
                version = '1.0.0-SNAPSHOT'

                artifactory {
                    publish {
                        repository {
                            maven = false
                        }
                        defaults {
                            publications('ivy')
                        }
                    }
                }

                subprojects {
                    group = 'com.intershop.testproject'
                    version = rootProject.getVersion()
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
                .withArguments('artifactoryPublish', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('snapshots/com.intershop.testproject/')
        }

        then:
        ! result.output.contains('CREATE JAVADOC')
        result.task(':artifactoryPublish').outcome == SUCCESS
        upLoadListCheck
        upLoadList.size() == 4
        upLoadListCheck

        where:
        buildFileContent << [buildFileContentBase]
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
