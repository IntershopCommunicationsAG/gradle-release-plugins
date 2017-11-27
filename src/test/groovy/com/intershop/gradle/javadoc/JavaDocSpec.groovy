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
package com.intershop.gradle.javadoc

import com.intershop.gradle.test.AbstractIntegrationSpec
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

@Unroll
class JavaDocSpec extends AbstractIntegrationSpec {

    def 'test javadoc generation (Gradle #gradleVersion)'(gradleVersion) {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                id 'com.intershop.gradle.javadoc-plugin'
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
                .withArguments('clean', 'javadoc', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        File fIndex = new File(testProjectDir, 'build/docs/javadoc/index.html')

        File fConstantValues = new File(testProjectDir, 'build/docs/javadoc/constant-values.html')
        File fDeprecatedList = new File(testProjectDir, 'build/docs/javadoc/deprecated-list.html')
        File fHelpDoc = new File(testProjectDir, 'build/docs/javadoc/help-doc.html')
        File fIndexAll = new File(testProjectDir, 'build/docs/javadoc/index-all.html')
        File fOverviewTree = new File(testProjectDir, 'build/docs/javadoc/overview-tree.html')

        then:
        fConstantValues.exists()
        fConstantValues.getText().contains('images/intershop_logo.gif')
        fDeprecatedList.exists()
        fDeprecatedList.getText().contains('images/intershop_logo.gif')
        fHelpDoc.exists()
        fHelpDoc.getText().contains('images/intershop_logo.gif')
        fIndexAll.exists()
        fIndexAll.getText().contains('images/intershop_logo.gif')
        fOverviewTree.exists()
        fOverviewTree.getText().contains('images/intershop_logo.gif')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test javadoc generation with changed build dir (Gradle #gradleVersion)'(gradleVersion) {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                id 'com.intershop.gradle.javadoc-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0'

            buildDir = new File(projectDir, 'target')

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
                .withArguments('clean', 'javadoc', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        File fIndex = new File(testProjectDir, 'build/docs/javadoc/index.html')

        File fConstantValues = new File(testProjectDir, 'target/docs/javadoc/constant-values.html')
        File fDeprecatedList = new File(testProjectDir, 'target/docs/javadoc/deprecated-list.html')
        File fHelpDoc = new File(testProjectDir, 'target/docs/javadoc/help-doc.html')
        File fIndexAll = new File(testProjectDir, 'target/docs/javadoc/index-all.html')
        File fOverviewTree = new File(testProjectDir, 'target/docs/javadoc/overview-tree.html')

        then:
        fConstantValues.exists()
        fConstantValues.getText().contains('images/intershop_logo.gif')
        fDeprecatedList.exists()
        fDeprecatedList.getText().contains('images/intershop_logo.gif')
        fHelpDoc.exists()
        fHelpDoc.getText().contains('images/intershop_logo.gif')
        fIndexAll.exists()
        fIndexAll.getText().contains('images/intershop_logo.gif')
        fOverviewTree.exists()
        fOverviewTree.getText().contains('images/intershop_logo.gif')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test javadoc publishing ivy (Gradle #gradleVersion)'(gradleVersion) {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                id 'com.intershop.gradle.javadoc-plugin'
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

        File fIndex = new File(testProjectDir, 'build/docs/javadoc/index.html')

        then:
        fIndex.exists()
        result.task(':publish').outcome == TaskOutcome.SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

}
