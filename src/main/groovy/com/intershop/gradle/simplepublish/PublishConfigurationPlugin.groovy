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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

import static com.intershop.gradle.util.PluginHelper.*

/**
 * This is the implementation of the plugin.
 */
@CompileStatic
class PublishConfigurationPlugin  implements Plugin<Project> {

    // Repo SNAPSHOT URL
    public final static String SNAPSHOT_URL_ENV = 'SNAPSHOTURL'
    public final static String SNAPSHOT_URL_PRJ = 'snapshotURL'

    // Repo RELEASE URL
    public final static String RELEASE_URL_ENV = 'RELEASEURL'
    public final static String RELEASE_URL_PRJ = 'releaseURL'

    /*
     * Repository user name
     */
    final static String REPO_USER_NAME_ENV = 'REPO_USER_NAME'
    final static String REPO_USER_NAME_PRJ = 'repoUserName'

    /*
     * Repository user password
     */
    final static String REPO_USER_PASSWD_ENV = 'REPO_USER_PASSWD'
    final static String REPO_USER_PASSWD_PRJ = 'repoUserPasswd'

    /**
     * SNAPSHOT RELEASE
     */
    final static String SNAPSHOT_RELEASE_ENV = 'SNAPSHOT_RELEASE'
    final static String SNAPSHOT_RELEASE_PRJ = 'snapshotRelease'

    // publication names
    public final static String MVNREPOName = 'intershopMvnCI'

    void apply(Project project) {
        project.logger.info('Simple release publishing configuration will be applied to project {}', project.name)

        String repoUserLogin = getVariable(project, REPO_USER_NAME_ENV, REPO_USER_NAME_PRJ, '')
        String repoUserPassword = getVariable(project, REPO_USER_PASSWD_ENV, REPO_USER_PASSWD_PRJ, '')

        String repoReleaseURL = getVariable(project, RELEASE_URL_ENV, RELEASE_URL_PRJ, '')
        String repoSnapshotURL = getVariable(project, SNAPSHOT_URL_ENV, SNAPSHOT_URL_PRJ, '')

        String snapshotRelease = getVariable(project, SNAPSHOT_RELEASE_ENV, SNAPSHOT_RELEASE_PRJ, 'false')

        if(repoUserLogin != '' && repoUserPassword != '' && repoReleaseURL != '' && repoSnapshotURL != '') {
            if (snapshotRelease.toLowerCase() == 'true') {
                project.version = "$project.version-SNAPSHOT"
            }

            project.afterEvaluate {
                if (repoSnapshotURL != '') {
                    project.subprojects.each {
                        applySnapshotPublishing(it, repoSnapshotURL, repoUserLogin, repoUserPassword, snapshotRelease.toLowerCase() == 'true')
                    }
                    applySnapshotPublishing(project, repoSnapshotURL, repoUserLogin, repoUserPassword, snapshotRelease.toLowerCase() == 'true')
                }
                if (repoReleaseURL != '') {
                    project.subprojects.each {
                        applyReleasePublishing(project, repoReleaseURL, repoUserLogin, repoUserPassword)
                    }
                    applyReleasePublishing(project, repoReleaseURL, repoUserLogin, repoUserPassword)
                }
            }
        }
    }

    @CompileDynamic
    private void applySnapshotPublishing(Project p, String snapshotURL, String repoUser, String repoUserPasswd, boolean useSnapShotRepo = false) {
        // the same configuration for maven publishing
        p.plugins.withType(MavenPublishPlugin) {
            p.publishing {
                repositories {
                    if (!delegate.findByName(MVNREPOName) && (p.version.endsWith('-SNAPSHOT') || useSnapShotRepo)) {
                        p.logger.info('Mvn publishing repository added for {}', snapshotURL)
                        maven {
                            name MVNREPOName
                            url snapshotURL
                            // only used for secured repositories
                            if (repoUser && repoUserPasswd) {
                                credentials {
                                    username repoUser
                                    password repoUserPasswd
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @CompileDynamic
    private void applyReleasePublishing(Project p, String releaseURL, String repoUser, String repoUserPasswd) {
        // the same configuration for maven publishing
        p.plugins.withType(MavenPublishPlugin) {
            p.publishing {
                repositories {
                    if (!delegate.findByName(MVNREPOName)) {
                        p.logger.info('Mvn publishing repository added for {}', releaseURL)
                        maven {
                            name MVNREPOName
                            url releaseURL
                            if (repoUser && repoUserPasswd) {
                                credentials {
                                    username repoUser
                                    password repoUserPasswd
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
