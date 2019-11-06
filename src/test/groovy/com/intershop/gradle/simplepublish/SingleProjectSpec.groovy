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


package com.intershop.gradle.simplepublish

import com.intershop.gradle.test.util.TestDispatcher
import com.intershop.gradle.test.AbstractIntegrationGroovySpec
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule

class SingleProjectSpec extends AbstractIntegrationGroovySpec {

    @Rule
    public final MockWebServer server = new MockWebServer()

    def 'test simple release publishing'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.simplepublish-configuration'
            }

            group = 'com.intershop'
            version = '1.0.0'

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
                .withArguments('publish', "-DRUNONCI=true", "-PsnapshotURL=${urlStr}nexus/snapshots", "-PreleaseURL=${urlStr}nexus/releases", '-PrepoUserName=admin', '-PrepoUserPasswd=admin123')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('/nexus/releases/')
        }

        then:
        upLoadListCheck
    }

    def 'test snapshot publishing'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.simplepublish-configuration'
            }

            group = 'com.intershop'
            version = '1.0.0-SNAPSHOT'

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
            rootProject.name = 'p_testProject'
        """.stripIndent()
        writeJavaTestClass("com.intershop.test")

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', "-DRUNONCI=true", "-PsnapshotURL=${urlStr}nexus/snapshots", "-PreleaseURL=${urlStr}nexus/releases", '-PrepoUserName=admin', '-PrepoUserPasswd=admin123', '-s')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('/nexus/snapshots')
        }

        then:
        upLoadListCheck
    }

    def 'test snapshot publishing with internal version changes'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        server.setDispatcher(TestDispatcher.getIntegrationDispatcher(responses, upLoadList))

        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.simplepublish-configuration'
            }

            group = 'com.intershop'
            version = '1.0.0'

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
            rootProject.name = 'p_testProject'
        """.stripIndent()
        writeJavaTestClass("com.intershop.test")

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', "-DRUNONCI=true", "-PsnapshotURL=${urlStr}nexus/snapshots", "-PreleaseURL=${urlStr}nexus/releases", '-PrepoUserName=admin', '-PrepoUserPasswd=admin123', '-PsnapshotRelease=true', '-s')
                .build()

        boolean upLoadListCheck = true

        upLoadList.each {
            upLoadListCheck &= it.contains('/nexus/snapshots')
        }

        then:
        upLoadListCheck
    }

    def 'test local publishing'() {
        given:
        String urlStr = server.url('/').toString()
        List<String> upLoadList = []
        Map<String,String> responses = [:]

        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.simplepublish-configuration'
            }

            group = 'com.intershop'
            version = '1.0.0'

            publishing {
                publications {
                    mvn(MavenPublication) {
                        from components.java
                    }
                }

                repositories {
                    maven {
                        url "\$buildDir/repo"
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
                .withArguments('publish', '-s')
                .build()

        then:
        (new File(testProjectDir, 'build/publications/mvn/pom-default.xml')).text.contains('1.0.0')
        (new File(testProjectDir, 'build/libs/p_testProject-1.0.0.jar')).exists()
    }

}
