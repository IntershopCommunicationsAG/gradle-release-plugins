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

import com.intershop.gradle.nexuspublish.TestDispatcher
import com.intershop.gradle.test.AbstractIntegrationSpec
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.Rule

class SingleProjectSpec extends AbstractIntegrationSpec {

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
                id 'ivy-publish'
                id 'com.intershop.gradle.simplepublish-configuration'
            }

            group = 'com.intershop'
            version = '1.0.0'

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
                id 'ivy-publish'
                id 'com.intershop.gradle.simplepublish-configuration'
            }

            group = 'com.intershop'
            version = '1.0.0-SNAPSHOT'

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
                .withArguments('publish', "-DRUNONCI=true", "-PsnapshotURL=${urlStr}nexus/snapshots", "-PreleaseURL=${urlStr}nexus/releases", '-PrepoUserName=admin', '-PrepoUserPasswd=admin123', '-s')
                .build()

        boolean upLoadListCheck = true
        upLoadList.each {
            upLoadListCheck &= it.contains('/nexus/snapshots')
        }

        then:
        upLoadListCheck
    }

}
