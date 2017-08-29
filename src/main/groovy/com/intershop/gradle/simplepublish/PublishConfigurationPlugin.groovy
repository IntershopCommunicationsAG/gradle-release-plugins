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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

import static com.intershop.gradle.util.PluginHelper.*

/**
 * This is the implementation of the plugin.
 */
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
    public final static String IVYREPONAME = 'intershopIvyCI'
    public final static String MVNREPOName = 'intershopMvnCI'

    public void apply(Project project) {
        String runOnCI = getVariable(project, RUNONCI_ENV, RUNONCI_PRJ, 'false')
        project.logger.info('Publishing Configuration: RunOnCI: {}', runOnCI.toBoolean())

        //provide compatibility with the repo config plung
        project.ext.useSCMVersionConfig = 'true'

        if (runOnCI.toBoolean()) {
            project.logger.info('Simple release publishing configuration will be applied to project {}', project.name)

            String repoUserLogin = getVariable(project, REPO_USER_NAME_ENV, REPO_USER_NAME_PRJ, '')
            String repoUserPassword = getVariable(project, REPO_USER_PASSWD_ENV, REPO_USER_PASSWD_PRJ, '')

            String repoReleaseURL = getVariable(project, RELEASE_URL_ENV, RELEASE_URL_PRJ, '')
            String repoSnapshotURL = getVariable(project, SNAPSHOT_URL_ENV, SNAPSHOT_URL_PRJ, '')

            String snapshotRelease = getVariable(project, SNAPSHOT_RELEASE_ENV, SNAPSHOT_RELEASE_PRJ, 'false')

            if(repoSnapshotURL != '') {
                if(project == project.rootProject) {
                        project.subprojects.each {
                            applySnapshotPublishing(it, repoSnapshotURL, repoUserLogin, repoUserPassword, snapshotRelease.toLowerCase() == 'true')
                        }

                }
                applySnapshotPublishing(project, repoSnapshotURL, repoUserLogin, repoUserPassword, snapshotRelease.toLowerCase() == 'true')
            }
            if(repoReleaseURL) {
                if(project == project.rootProject) {
                    project.subprojects.each {
                        applyReleasePublishing(it, repoReleaseURL, repoUserLogin, repoUserPassword)
                    }
                }
                applyReleasePublishing(project, repoReleaseURL, repoUserLogin, repoUserPassword)
            }

            project.rootProject.afterEvaluate {
                if(snapshotRelease.toLowerCase() == 'true') {
                    project.version = "$project.version-SNAPSHOT"
                }
                if(! project.version.endsWith('-SNAPSHOT')) {
                    // add javadoc to root project
                    project.getRootProject().ext.releaseWithJavaDoc = 'true'
                    // add javadoc to sub project
                    project.getRootProject().getSubprojects().each { Project subp ->
                        subp.ext.releaseWithJavaDoc = 'true'
                    }
                }
            }
        } else {
            project.rootProject.afterEvaluate {
                project.logger.info('Project runs local! Local configuration will be set.')
                project.status = 'local'
                project.version = "$project.version-LOCAL"
            }
        }
    }

    private void applySnapshotPublishing(Project p, String snapshotURL, String repoUser, String repoUserPasswd, boolean useSnapShotRepo = false) {
        p.plugins.withType(IvyPublishPlugin) {
            p.publishing {
                repositories {
                    if (!delegate.findByName(IVYREPONAME) && (p.version.endsWith('-SNAPSHOT') || useSnapShotRepo)) {
                        p.logger.info('Add Ivy publishing repository')
                        ivy {
                            name IVYREPONAME
                            // Configuration for snapshots - publishing to repository
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
            p.repositories {
                if (!delegate.findByName(IVYREPONAME) && (p.version.endsWith('-SNAPSHOT') || useSnapShotRepo)) {
                    p.logger.info('Ivy repository {} added to the project repository configuration', IVYREPONAME)
                    ivy {
                        name IVYREPONAME
                        url snapshotURL
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
        // the same configuration for maven publishing
        p.plugins.withType(MavenPublishPlugin) {
            p.publishing {
                repositories {
                    if (!delegate.findByName(MVNREPOName) && (p.version.endsWith('-SNAPSHOT') || useSnapShotRepo)) {
                        p.logger.info('Add Mvn publishing repository')
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
            p.repositories {
                if (!delegate.findByName(MVNREPOName) && (p.version.endsWith('-SNAPSHOT') || useSnapShotRepo)) {
                    p.logger.info('Mvn repository {} added to the project repository configuration', MVNREPOName)
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

    private void applyReleasePublishing(Project p, String releaseURL, String repoUser, String repoUserPasswd) {
        p.plugins.withType(IvyPublishPlugin) {
            p.publishing {
                repositories {
                    if (!delegate.findByName(IVYREPONAME)) {
                        p.logger.info('Direct Ivy publishing repository added for {}', releaseURL)
                        ivy {
                            name IVYREPONAME
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
            p.repositories {
                if (!delegate.findByName(IVYREPONAME)) {
                    p.logger.info('Direct Ivy repository added for {}', releaseURL)
                    ivy {
                        name IVYREPONAME
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

        // the same configuration for maven publishing
        p.plugins.withType(MavenPublishPlugin) {
            p.publishing {
                repositories {
                    if (!delegate.findByName(MVNREPOName)) {
                        p.logger.info('Direct Mvn publishing repository added for {}', releaseURL)
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
            p.repositories {
                if (!delegate.findByName(MVNREPOName)) {
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
