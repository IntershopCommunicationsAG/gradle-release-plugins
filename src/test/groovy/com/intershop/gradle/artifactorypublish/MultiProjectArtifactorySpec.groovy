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

import com.intershop.gradle.test.AbstractIntegrationGroovySpec
import com.intershop.gradle.test.util.TestDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

@Unroll
class MultiProjectArtifactorySpec extends AbstractIntegrationGroovySpec {

    static String issueKey = 'ISTOOLS-993'

    static String pluginConfig = """
                  id 'com.intershop.gradle.scmversion' version '6.1.0'
                  id 'com.intershop.gradle.artifactorypublish-configuration'
    """.stripIndent()

    String configURL = System.properties['configURL']
    String configToken = System.properties['configURLToken']

    private static String buildFileContentBase = """
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

    def 'test release publishing with artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
                plugins {
                  id 'maven-publish'
                  ${pluginConfig}
                }

                group = 'com.intershop.testproject'
                version = '1.0.0'

                artifactory {
                    publish {
                        defaults {
                            publications('maven')
                            properties = ['testBla': 'testBla']
                        }
                    }
                }

                subprojects {
                    group = 'com.intershop.testproject'
                    version = rootProject.getVersion()
                }
        """.stripIndent()

        settingsFile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()
        createSubProjectJava('project1a','com.intereshop.a', buildFileContent, '1.0.0')
        createSubProjectJava('project2b', 'com.intereshop.b', buildFileContent, '1.0.0')

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
                .withArguments('artifactoryPublish', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com/intershop/testproject/')
        }

        then:
        result.task(':artifactoryPublish').outcome == SUCCESS
        result.getTasks().findAll( { it.path == ':setIssueField'} ).isEmpty()
        upLoadListCheck
        upLoadList.size() > 0

        where:
        buildFileContent << [buildFileContentBase]
    }

    def 'test release log with artifactory configuration'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
                plugins {
                  id 'maven-publish'
                  ${pluginConfig}
                }

                group = 'com.intershop.testproject'
                version = '1.0.0'

                artifactory {
                    publish {
                        defaults {
                            publications('maven')
                            properties = ['testBla': 'testBla']
                        }
                    }
                }

                subprojects {
                    group = 'com.intershop.testproject'
                    version = rootProject.getVersion()
                }
                
                repositories {
                    jcenter()
                }
        """.stripIndent()

        settingsFile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()
        createSubProjectJava('project1a', 'com.intereshop.a', buildFileContent, '1.0.0')
        createSubProjectJava('project2b', 'com.intereshop.b', buildFileContent, '1.0.0')

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
                .withArguments('releaseLog', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com/intershop/testproject/')
        }

        then:
        result.task(':setIssueField').outcome == SUCCESS
        responses.get('onebody').contains('"project":{"key":"ISTOOLS"}')
        responses.get('onebody').contains('"issuetype":{"id":"10001"}')

        where:
        buildFileContent << [buildFileContentBase]
    }

    def 'test publishing without artifactory'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
                plugins {
                  id 'maven-publish'
                  ${pluginConfig}
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

        settingsFile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()
        createSubProjectJava('project1a', 'com.intereshop.a', buildFileContent, '1.0.0')
        createSubProjectJava('project2b', 'com.intereshop.b', buildFileContent, '1.0.0')

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
                .withArguments('publish', '--stacktrace', '-i')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('releases/com/intershop/testproject/')
        }

        then:
        ! result.tasks.contains(':setIssueField')
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
                  id 'maven-publish'
                  ${pluginConfig}
                }

                group = 'com.intershop.testproject'
                version = '1.0.0-SNAPSHOT'

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

        settingsFile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()
        createSubProjectJava('project1a', 'com.intereshop.a', buildFileContent, '1.0.0-SNAPSHOT')
        createSubProjectJava('project2b', 'com.intereshop.b', buildFileContent, '1.0.0-SNAPSHOT')

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
                .withArguments('artifactoryPublish', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123', '-Dhttp.proxyHost=localhost', '-Dhttp.proxyPort=8200')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('snapshots/com/intershop/testproject/')
        }

        then:
        result.task(':artifactoryPublish').outcome == SUCCESS
        upLoadListCheck
        upLoadList.size() > 3
        ! result.tasks.contains(':writeToJira')

        where:
        buildFileContent << [buildFileContentBase]
    }

    def 'test snapshot publishing with artifactory configuration and release log'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
                plugins {
                  id 'maven-publish'
                  ${pluginConfig}
                }

                group = 'com.intershop.testproject'
                version = '1.0.0-SNAPSHOT'

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

        settingsFile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()
        createSubProjectJava('project1a', 'com.intereshop.a', buildFileContent, '1.0.0-SNAPSHOT')
        createSubProjectJava('project2b', 'com.intereshop.b', buildFileContent, '1.0.0-SNAPSHOT')

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
                .withArguments('releaseLog', '--exclude-task', 'changelog', '--stacktrace', '-i', "-DRUNONCI=true", '-PjiraFieldName=Labels', "-DARTIFACTORYBASEURL=${urlStr}", '-DSNAPSHOTREPOKEY=snapshots', '-DRELEASEREPOKEY=releases', '-DARTIFACTORYUSERNAME=admin', '-DARTIFACTORYUSERPASSWD=admin123', "-DJIRABASEURL=${urlStr}", '-DJIRAUSERNAME=admin', '-DJIRAUSERPASSWD=admin123', '-Dhttp.proxyHost=localhost', '-Dhttp.proxyPort=8200')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('snapshots/com/intershop/testproject/')
        }

        then:
        result.task(':releaseLog').outcome == UP_TO_DATE
        ! result.tasks.contains(':writeToJira')

        where:
        buildFileContent << [buildFileContentBase]
    }

    /**
     * Creates a java sub project
     */
    private File createSubProjectJava(String projectPath, String packageName, String buildContent, String version){
        File subProject = createSubProject(projectPath, buildContent.replace('VERSION', version))
        writeJavaTestClass(packageName, subProject)
        return subProject
    }
}
