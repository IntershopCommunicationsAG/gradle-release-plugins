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
package com.intershop.gradle.javasource

import com.intershop.gradle.test.AbstractIntegrationSpec
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

@Unroll
class IntJavaSourcePluginSpec extends AbstractIntegrationSpec {

    def 'test java source package with single project and ivy (Gradle #gradleVersion)'(gradleVersion) {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                id 'com.intershop.gradle.javasource-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0'

            publishing {
                repositories {
                    ivy {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
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
                .withArguments('clean', 'publish', '--stacktrace', '-i', '-PrunOnCI=true')
                .withGradleVersion(gradleVersion)
                .build()

        File pubSourceJar = new File(testProjectDir,'build/repo/com.intershop/testProject/1.0.0/testProject-1.0.0-sources.jar')
        File ivyFile = new File(testProjectDir, 'build/repo/com.intershop/testProject/1.0.0/ivy-1.0.0.xml')

        then:
        result.task(':publish').outcome == TaskOutcome.SUCCESS
        result.task(':sourceJar').outcome == TaskOutcome.SUCCESS
        pubSourceJar.exists()
        ivyFile.exists()
        ivyFile.text.contains('artifact name="testProject" type="sources"')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test java source package with single project and maven (Gradle #gradleVersion)'(gradleVersion) {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.javasource-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0'

            publishing {
                repositories {
                    maven {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    mvn(MavenPublication) {
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
                .withArguments('clean', 'publish', '--stacktrace', '-i', '-PrunOnCI=true')
                .withGradleVersion(gradleVersion)
                .build()

        File pubSourceJar = new File(testProjectDir,'build/repo/com/intershop/testProject/1.0.0/testProject-1.0.0-sources.jar')

        then:
        result.task(':publish').outcome == TaskOutcome.SUCCESS
        result.task(':sourceJar').outcome == TaskOutcome.SUCCESS
        pubSourceJar.exists()

        where:
        gradleVersion << supportedGradleVersions
    }
}
