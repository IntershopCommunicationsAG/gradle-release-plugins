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


package com.intershop.gradle.escrow

import com.intershop.gradle.test.AbstractIntegrationSpec
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

import java.util.zip.ZipFile

class EscrowPluginSpec extends AbstractIntegrationSpec {

    def 'ivy - test simple escrow file for multiproject'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'ivy-publish'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
        }

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')
        ZipFile fZipZip = new ZipFile(fZip)

        List fZipPath = []
        fZipZip.entries().each {
            fZipPath.add(it.getName())
        }
        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('IvyEscrow')

        fZip.exists()
        fIvy.exists()

        fZipPath.contains('testProject/build.gradle')
        fZipPath.contains('testProject/settings.gradle')
        fZipPath.contains('testProject/subproject1/build.gradle')
        fZipPath.contains('testProject/subproject1/src/main/java/com/intershop/test1/HelloWorld.java')
        fZipPath.contains('testProject/subproject2/build.gradle')
        fZipPath.contains('testProject/subproject2/src/main/java/com/intershop/test2/HelloWorld.java')
        fZipPath.contains('testProject/subproject3/build.gradle')
        fZipPath.contains('testProject/subproject3/src/main/java/com/intershop/test3/HelloWorld.java')
        ! fZipPath.contains('testProject/.gradle')
        ! fZipPath.contains('testProject/subproject1/.gradle')
        ! fZipPath.contains('testProject/subproject2/.gradle')
        ! fZipPath.contains('testProject/subproject3/.gradle')
        ! fZipPath.contains('testProject/build')
        ! fZipPath.contains('testProject/subproject1/build')
        ! fZipPath.contains('testProject/subproject2/build')
        ! fZipPath.contains('testProject/subproject3/build')
    }

    def 'ivy - test simple escrow file for multiproject - snapshot version'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'ivy-publish'

                group = 'com.intershop'
                version = '1.0.0-SNAPSHOT'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
        }

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0-SNAPSHOT'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')

        then:
        ! fZip.exists()
        ! fIvy.exists()

        ! result.output.contains('IvyEscrow')
    }

    def 'ivy - test simple escrow file for multiproject - local'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'ivy-publish'

                group = 'com.intershop'
                version = '1.0.0-LOCAL'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
        }

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0-LOCAL'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')

        then:
        ! fZip.exists()
        ! fIvy.exists()

        ! result.output.contains('IvyEscrow')
    }

    def 'mvn - test simple escrow file for multiproject'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'maven-publish'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
        }

        buildFile << """
            plugins {
                id 'maven-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

            publishing {
                repositories {
                    maven {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fPom = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0.pom')
        ZipFile fZipZip = new ZipFile(fZip)

        List fZipPath = []
        fZipZip.entries().each {
            fZipPath.add(it.getName())
        }
        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('MvnEscrow')

        fZip.exists()
        fPom.exists()

        fZipPath.contains('testProject/build.gradle')
        fZipPath.contains('testProject/settings.gradle')
        fZipPath.contains('testProject/subproject1/build.gradle')
        fZipPath.contains('testProject/subproject1/src/main/java/com/intershop/test1/HelloWorld.java')
        fZipPath.contains('testProject/subproject2/build.gradle')
        fZipPath.contains('testProject/subproject2/src/main/java/com/intershop/test2/HelloWorld.java')
        fZipPath.contains('testProject/subproject3/build.gradle')
        fZipPath.contains('testProject/subproject3/src/main/java/com/intershop/test3/HelloWorld.java')
        ! fZipPath.contains('testProject/.gradle')
        ! fZipPath.contains('testProject/subproject1/.gradle')
        ! fZipPath.contains('testProject/subproject2/.gradle')
        ! fZipPath.contains('testProject/subproject3/.gradle')
        ! fZipPath.contains('testProject/build')
        ! fZipPath.contains('testProject/subproject1/build')
        ! fZipPath.contains('testProject/subproject2/build')
        ! fZipPath.contains('testProject/subproject3/build')
    }

    def 'mvn - test simple escrow file for multiproject - snapshot version'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'maven-publish'

                group = 'com.intershop'
                version = '1.0.0-SNAPSHOT'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
        }

        buildFile << """
            plugins {
                id 'maven-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0-SNAPSHOT'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

            publishing {
                repositories {
                    maven {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fPom = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0.pom')

        then:
        ! result.output.contains('MvnEscrow')

        ! fZip.exists()
        ! fPom.exists()
    }

    def 'mvn - test simple escrow file for multiproject - local'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'maven-publish'

                group = 'com.intershop'
                version = '1.0.0-LOCAL'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
        }

        buildFile << """
            plugins {
                id 'maven-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0-LOCAL'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

            publishing {
                repositories {
                    maven {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fPom = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0.pom')

        then:
        ! result.output.contains('MvnEscrow')

        ! fZip.exists()
        ! fPom.exists()
    }

    def 'ivy - test simple escrow file for single project'() {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')
        ZipFile fZipZip = new ZipFile(fZip)

        List fZipPath = []
        fZipZip.entries().each {
            fZipPath.add(it.getName())
        }

        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('IvyEscrow')

        fZip.exists()
        fIvy.exists()

        fZipPath.contains('testProject/build.gradle')
        fZipPath.contains('testProject/settings.gradle')
        fZipPath.contains('testProject/src/main/java/com/intershop/test/HelloWorld.java')

        ! fZipPath.contains('testProject/.gradle')
        ! fZipPath.contains('testProject/build')
    }

    def 'ivy - test simple escrow file for single project - snapshot version'() {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0-SNAPSHOT'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')

        then:
        ! result.output.contains('IvyEscrow')

        ! fZip.exists()
        ! fIvy.exists()
    }

    def 'ivy - test simple escrow file for single project - local'() {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0-LOCAL'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')

        then:
        ! result.output.contains('IvyEscrow')

        ! fZip.exists()
        ! fIvy.exists()
    }

    def 'mvn - test simple escrow file for single project'() {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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

        writeJavaTestClass('com.intershop.test')

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fPom = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0.pom')
        ZipFile fZipZip = new ZipFile(fZip)

        List fZipPath = []
        fZipZip.entries().each {
            fZipPath.add(it.getName())
        }

        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('MvnEscrow')

        fZip.exists()
        fPom.exists()

        fZipPath.contains('testProject/build.gradle')
        fZipPath.contains('testProject/settings.gradle')
        fZipPath.contains('testProject/src/main/java/com/intershop/test/HelloWorld.java')

        ! fZipPath.contains('testProject/.gradle')
        ! fZipPath.contains('testProject/build')
    }

    def 'mvn - test simple escrow file for single project - snapshot version'() {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0-SNAPSHOT'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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

        writeJavaTestClass('com.intershop.test')

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fPom = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0.pom')

        then:
        ! result.output.contains('MvnEscrow')

        ! fZip.exists()
        ! fPom.exists()
    }

    def 'mvn - test simple escrow file for single project - local'() {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
            }

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

        writeJavaTestClass('com.intershop.test')

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fPom = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0.pom')

        then:
        ! result.output.contains('MvnEscrow')

        ! fZip.exists()
        ! fPom.exists()
    }

    def 'ivy - test simple escrow file for single project - classifier changed'() {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
                classifier = 'escrow'
            }

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
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-escrow.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')

        def ivyXML = new XmlSlurper(false,false).parse(fIvy)

        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('IvyEscrow')

        fZip.exists()
        fIvy.exists()

        ivyXML.publications[0].artifact[0].'@m:classifier' == 'escrow'
    }

    def 'mvn - test simple escrow file for single project - classifier changed'() {
        given:
        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            group = 'com.intershop'
            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
                classifier = 'escrow'
            }

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

        writeJavaTestClass('com.intershop.test')

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0-escrow.zip')
        File fPom = new File(testProjectDir, 'build/repo/com/intershop/source/testProject/1.0.0/testProject-1.0.0.pom')

        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('MvnEscrow')

        fZip.exists()
        fPom.exists()
    }

    def 'ivy - test simple escrow file for multiproject - exclude'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'ivy-publish'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
            file('projectHelper.properties', new File(testProjectDir, "subproject${it}")) << 'test = test'
        }

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
                exclude('**/**/projectHelper.properties')
            }

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
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')
        ZipFile fZipZip = new ZipFile(fZip)

        List fZipPath = []
        fZipZip.entries().each {
            fZipPath.add(it.getName())
        }
        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('IvyEscrow')

        fZip.exists()
        fIvy.exists()

        fZipPath.contains('testProject/build.gradle')
        fZipPath.contains('testProject/settings.gradle')
        fZipPath.contains('testProject/subproject1/build.gradle')
        fZipPath.contains('testProject/subproject1/src/main/java/com/intershop/test1/HelloWorld.java')
        fZipPath.contains('testProject/subproject2/build.gradle')
        fZipPath.contains('testProject/subproject2/src/main/java/com/intershop/test2/HelloWorld.java')
        fZipPath.contains('testProject/subproject3/build.gradle')
        fZipPath.contains('testProject/subproject3/src/main/java/com/intershop/test3/HelloWorld.java')
        ! fZipPath.contains('testProject/.gradle')
        ! fZipPath.contains('testProject/subproject1/.gradle')
        ! fZipPath.contains('testProject/subproject2/.gradle')
        ! fZipPath.contains('testProject/subproject3/.gradle')
        ! fZipPath.contains('testProject/build')
        ! fZipPath.contains('testProject/subproject1/build')
        ! fZipPath.contains('testProject/subproject2/build')
        ! fZipPath.contains('testProject/subproject3/build')
        ! fZipPath.contains('testProject/subproject1/projectHelper.properties')
        ! fZipPath.contains('testProject/subproject2/projectHelper.properties')
        ! fZipPath.contains('testProject/subproject3/projectHelper.properties')
    }

    def 'ivy - test simple escrow file for multiproject - excludes'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'ivy-publish'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
            file('projectHelper1.properties', new File(testProjectDir, "subproject${it}")) << 'test1 = test1'
            file('projectHelper2.properties', new File(testProjectDir, "subproject${it}")) << 'test2 = test2'
        }

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
                excludes(['**/projectHelper1.properties', '**/projectHelper2.properties'])
            }

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
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')
        ZipFile fZipZip = new ZipFile(fZip)

        List fZipPath = []
        fZipZip.entries().each {
            fZipPath.add(it.getName())
        }
        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('IvyEscrow')

        fZip.exists()
        fIvy.exists()

        fZipPath.contains('testProject/build.gradle')
        fZipPath.contains('testProject/settings.gradle')
        fZipPath.contains('testProject/subproject1/build.gradle')
        fZipPath.contains('testProject/subproject1/src/main/java/com/intershop/test1/HelloWorld.java')
        fZipPath.contains('testProject/subproject2/build.gradle')
        fZipPath.contains('testProject/subproject2/src/main/java/com/intershop/test2/HelloWorld.java')
        fZipPath.contains('testProject/subproject3/build.gradle')
        fZipPath.contains('testProject/subproject3/src/main/java/com/intershop/test3/HelloWorld.java')
        ! fZipPath.contains('testProject/.gradle')
        ! fZipPath.contains('testProject/subproject1/.gradle')
        ! fZipPath.contains('testProject/subproject2/.gradle')
        ! fZipPath.contains('testProject/subproject3/.gradle')
        ! fZipPath.contains('testProject/build')
        ! fZipPath.contains('testProject/subproject1/build')
        ! fZipPath.contains('testProject/subproject2/build')
        ! fZipPath.contains('testProject/subproject3/build')
        ! fZipPath.contains('testProject/subproject1/projectHelper1.properties')
        ! fZipPath.contains('testProject/subproject2/projectHelper1.properties')
        ! fZipPath.contains('testProject/subproject3/projectHelper1.properties')
        ! fZipPath.contains('testProject/subproject1/projectHelper2.properties')
        ! fZipPath.contains('testProject/subproject2/projectHelper2.properties')
        ! fZipPath.contains('testProject/subproject3/projectHelper2.properties')
    }

    def 'ivy - test simple escrow file for multiproject - setExcludes'() {
        given:
        [1,2,3].each {
            File bf = file("subproject${it}/build.gradle")
            bf << """
                apply plugin: 'java'
                apply plugin: 'ivy-publish'

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
            writeJavaTestClass("com.intershop.test${it}", new File(testProjectDir, "subproject${it}"))
            file('projectHelper1.properties', new File(testProjectDir, "subproject${it}")) << 'test1 = test1'
            file('projectHelper2.properties', new File(testProjectDir, "subproject${it}")) << 'test2 = test2'
            file('build/test.test', new File(testProjectDir, "subproject${it}")) << 'test1 = test1'
        }

        buildFile << """
            plugins {
                id 'ivy-publish'
                id 'com.intershop.gradle.escrow-plugin'
            }

            version = '1.0.0'

            escrow {
                sourceGroup = 'com.intershop.source'
                setExcludes(['.gradle', '**/projectHelper1.properties', '**/projectHelper2.properties'])
            }

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
            rootProject.name = 'testProject'

            include 'subproject1'
            include 'subproject2'
            include 'subproject3'
        """.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('publish', '-PrunOnCI=true', '--stacktrace', '-i')
                .withPluginClasspath(pluginClasspath)
                .build()

        File fZip = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/testProject-1.0.0-src.zip')
        File fIvy = new File(testProjectDir, 'build/repo/com.intershop.source/testProject/1.0.0/ivy-1.0.0.xml')
        ZipFile fZipZip = new ZipFile(fZip)

        List fZipPath = []
        fZipZip.entries().each {
            fZipPath.add(it.getName())
        }
        then:
        result.task(':escrowZip').outcome == TaskOutcome.SUCCESS
        result.output.contains('IvyEscrow')

        fZip.exists()
        fIvy.exists()

        fZipPath.contains('testProject/build.gradle')
        fZipPath.contains('testProject/settings.gradle')
        fZipPath.contains('testProject/subproject1/build.gradle')
        fZipPath.contains('testProject/subproject1/src/main/java/com/intershop/test1/HelloWorld.java')
        fZipPath.contains('testProject/subproject2/build.gradle')
        fZipPath.contains('testProject/subproject2/src/main/java/com/intershop/test2/HelloWorld.java')
        fZipPath.contains('testProject/subproject3/build.gradle')
        fZipPath.contains('testProject/subproject3/src/main/java/com/intershop/test3/HelloWorld.java')
        fZipPath.contains('testProject/build/')
        fZipPath.contains('testProject/subproject1/build/')
        fZipPath.contains('testProject/subproject2/build/')
        fZipPath.contains('testProject/subproject3/build/')

        ! fZipPath.contains('testProject/subproject1/projectHelper1.properties')
        ! fZipPath.contains('testProject/subproject2/projectHelper1.properties')
        ! fZipPath.contains('testProject/subproject3/projectHelper1.properties')
        ! fZipPath.contains('testProject/subproject1/projectHelper2.properties')
        ! fZipPath.contains('testProject/subproject2/projectHelper2.properties')
        ! fZipPath.contains('testProject/subproject3/projectHelper2.properties')
    }
}
