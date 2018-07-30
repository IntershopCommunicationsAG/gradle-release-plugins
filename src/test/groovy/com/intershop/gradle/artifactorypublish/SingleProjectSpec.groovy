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
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class SingleProjectSpec extends AbstractIntegrationSpec {

    static String issueKey = 'ISTOOLS-993'
    static String pluginConfig = """
                  id 'com.intershop.gradle.scmversion' version '3.6.0'
                  id 'com.intershop.gradle.artifactorypublish-configuration'
    """.stripIndent()

    @Rule
    public final MockWebServer server = new MockWebServer()

    def 'test release publishing with artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

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
                .withArguments('artifactoryPublish', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com.intershop/')
        }

        then:
        upLoadListCheck
        upLoadList.size() == 2
        result.getTasks().findAll( { it.path == ':setIssueField'} ).isEmpty()
        //result.task(':setIssueField').outcome == SUCCESS
        result.task(':artifactoryPublish').outcome == SUCCESS
        //responses.get('onebody').contains('"project":{"key":"ISTOOLS"}')
        //responses.get('onebody').contains('"issuetype":{"id":"10001"}')
    }

    def 'test release log with artifactory confioguration'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

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

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                ${pluginConfig}
            }

            repositories {
                jcenter()
            }
                
            group = 'com.intershop'
            version = '1.1.0'

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
            rootProject.name = 'testProject'
        """.stripIndent()
        writeJavaTestClass("com.intershop.test")

        when:
        def result = getPreparedGradleRunner()
                .withArguments('releaseLog', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com.intershop/')
        }

        then:
        result.task(':setIssueField').outcome == SUCCESS
        result.task(':releaseLog').outcome == SUCCESS
        responses.get('onebody').contains('"project":{"key":"ISTOOLS"}')
        responses.get('onebody').contains('"issuetype":{"id":"10001"}')
    }

    def 'test release publishing with artifactory and maven'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

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
                .withArguments('artifactoryPublish', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com/intershop/')
        }

        then:
        upLoadListCheck
        upLoadList.size() == 2
        result.getTasks().findAll( { it.path == ':setIssueField'} ).isEmpty()
        //result.task(':setIssueField').outcome == SUCCESS
        result.task(':artifactoryPublish').outcome == SUCCESS
        //responses.get('onebody').contains('"project":{"key":"ISTOOLS"}')
        //responses.get('onebody').contains('"issuetype":{"id":"10001"}')
    }


    def 'test snapshot publishing with artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

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
                .withArguments('artifactoryPublish', '--exclude-task', 'changelog', '--stacktrace', '-d', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('snapshots/com.intershop/')
        }

        then:
        upLoadListCheck
        upLoadList.size() == 2
        result.task(':artifactoryPublish').outcome == SUCCESS
        ! result.tasks.contains(':writeToJira')
    }

    def 'test snapshot release log with artifactory configuration'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

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
                .withArguments('releaseLog', '--exclude-task', 'changelog', '--stacktrace', '-d', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('snapshots/com.intershop/')
        }

        then:
        result.task(':releaseLog').outcome == UP_TO_DATE
    }
}
