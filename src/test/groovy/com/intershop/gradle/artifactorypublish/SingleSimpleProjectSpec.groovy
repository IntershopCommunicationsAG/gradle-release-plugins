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


package com.intershop.gradle.artifactorypublish

import com.intershop.gradle.test.AbstractIntegrationSpec
import com.intershop.gradle.test.util.TestDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class SingleSimpleProjectSpec extends AbstractIntegrationSpec {

    static String pluginConfig = """
                  id 'com.intershop.gradle.scmversion' version '3.6.0'
                  id 'com.intershop.gradle.simpleartifactorypublish-configuration'
    """.stripIndent()

    @Rule
    public final MockWebServer server = new MockWebServer()

    def 'test release publishing with artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                ${pluginConfig}
            }

            group = 'com.intershop'
            version = '1.1.0'

            artifactory {
                publish {
                    repository {
                        //repoKey = ''
                        maven = false
                    }
                    defaults {
                        publications('ivy')
                    }
                }
            }

            publishing {
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()
        writeJavaTestClass("com.intershop.test")

        when:
        def result = getPreparedGradleRunner()
                .withArguments('artifactoryPublish', '--stacktrace', '-i', "-DRUNONCI=true", "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com.intershop/')
        }

        then:
        upLoadListCheck
        upLoadList.size() == 2
        result.task(':artifactoryPublish').outcome == SUCCESS
    }

    def 'test release publishing with artifactory and maven'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                ${pluginConfig}
            }

            group = 'com.intershop'
            version = '1.1.0'

            artifactory {
                publish {
                    defaults {
                        publications('maven')
                    }
                }
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()
        writeJavaTestClass("com.intershop.test")

        when:
        def result = getPreparedGradleRunner()
                .withArguments('artifactoryPublish', '--stacktrace', '-i', "-DRUNONCI=true", "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com/intershop/')
        }

        then:
        upLoadListCheck
        upLoadList.size() == 2
        result.task(':artifactoryPublish').outcome == SUCCESS
    }


    def 'test snapshot publishing with artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                ${pluginConfig}
            }

            group = 'com.intershop'
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

            publishing {
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()
        writeJavaTestClass("com.intershop.test")

        when:
        def result = getPreparedGradleRunner()
                .withArguments('artifactoryPublish', '--stacktrace', '-d', "-DRUNONCI=true", "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('snapshots/com.intershop/')
        }

        then:
        upLoadListCheck
        upLoadList.size() == 2
        result.task(':artifactoryPublish').outcome == SUCCESS
    }

}
