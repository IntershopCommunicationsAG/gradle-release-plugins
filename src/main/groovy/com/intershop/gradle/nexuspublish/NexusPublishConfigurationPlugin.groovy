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
package com.intershop.gradle.nexuspublish

import com.intershop.gradle.jiraconnector.JiraConnectorPlugin
import com.intershop.gradle.nexusstaging.NexusStagingPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

import static com.intershop.gradle.util.PluginHelper.*

/**
 * This is the implementation of the plugin.
 */
class NexusPublishConfigurationPlugin implements Plugin<Project> {

    /** Repository Configuration - Start **/
    // Repo SNAPSHOT URL
    public final static String SNAPSHOT_URL_ENV = 'SNAPSHOTURL'
    public final static String SNAPSHOT_URL_PRJ = 'snapshotURL'

    // Repo RELEASE URL
    public final static String RELEASE_URL_ENV = 'RELEASEURL'
    public final static String RELEASE_URL_PRJ = 'releaseURL'

    // Repo SNAPSHOT Path (based on Nexus Base URL configuration)
    public final static String SNAPSHOT_PATH_ENV = 'SNAPSHOTPATH'
    public final static String SNAPSHOT_PATH_PRJ = 'snapshotPath'

    // Repo RELEASE Path (based on Nexus Base URL configuration)
    public final static String RELEASE_PATH_ENV = 'RELEASEPATH'
    public final static String RELEASE_PATH_PRJ = 'releasePath'

    // dublicated from nexusstaging-gradle-plugin
    public final static String REPO_BASEURL_ENV = 'NEXUSBASEURL'
    public final static String REPO_BASEURL_PRJ = 'nexusBaseURL'

    public final static String REPO_USER_NAME_ENV = 'NEXUSUSERNAME'
    public final static String REPO_USER_NAME_PRJ = 'nexusUserName'

    public final static String REPO_USER_PASSWORD_ENV = 'NEXUSUSERPASSWD'
    public final static String REPO_USER_PASSWORD_PRJ = 'nexusUserPASSWD'

    /** Repository Configuration - End **/

    /** Jira configuration - start **/
    public final static String JIRA_BASEURL_ENV = 'JIRABASEURL'
    public final static String JIRA_BASEURL_PRJ = 'jiraBaseURL'

    public final static String JIRA_USER_NAME_ENV = 'JIRAUSERNAME'
    public final static String JIRA_USER_NAME_PRJ = 'jiraUserName'

    public final static String JIRA_USER_PASSWORD_ENV = 'JIRAUSERPASSWD'
    public final static String JIRA_USER_PASSWORD_PRJ = 'jiraUserPASSWD'

    /** Jira configuration - End **/

    // publication names
    public final static String IVYREPONAME = 'intershopIvyCI'
    public final static String MVNREPOName = 'intershopMvnCI'



    private String getURL(String baseURL, String path) {
        String tmpBaseURL = (baseURL && baseURL.endsWith('/')) ? baseURL.substring(0, baseURL.length() - 1) : baseURL
        String tmpPath = (path && path.startsWith('/')) ? path.substring(1) : path

        return "${tmpBaseURL}/${tmpPath}"
    }


    public void apply(Project project) {

        String runOnCI = getVariable(project, RUNONCI_ENV, RUNONCI_PRJ, 'false')
        project.logger.info('Publishing Configuration: RunOnCI: {}', runOnCI.toBoolean())

        if (runOnCI.toBoolean()) {
            project.logger.info('Intershop release publishing configuration will be applied to project {}', project.name)

            if(! project.rootProject.tasks.findByName('changelog')) {
                throw new GradleException('Please apply also "com.intershop.gradle.scmversion"')
            }

            String jiraFieldName = project.hasProperty('jiraFieldName') ? project.property('jiraFieldName') : 'Fix Version/s'

            String repoBaseURL = getVariable(project, REPO_BASEURL_ENV, REPO_BASEURL_PRJ, '')
            String repoUserLogin = getVariable(project, REPO_USER_NAME_ENV, REPO_USER_NAME_PRJ, '')
            String repoUserPassword = getVariable(project, REPO_USER_PASSWORD_ENV, REPO_USER_PASSWORD_PRJ, '')

            String repoReleasePath = getVariable(project, RELEASE_PATH_ENV, RELEASE_PATH_PRJ, '')
            String repoSnapshotPath = getVariable(project,SNAPSHOT_PATH_ENV, SNAPSHOT_PATH_PRJ, 'content/repositories/snapshots/')

            String repoReleaseURL = getVariable(project, RELEASE_URL_ENV, RELEASE_URL_PRJ, '')
            String repoSnapshotURL = getVariable(project, SNAPSHOT_URL_ENV, SNAPSHOT_URL_PRJ, getURL(repoBaseURL, repoSnapshotPath))

            if((repoBaseURL && repoReleasePath) || repoReleaseURL) {
                String repo = repoBaseURL && repoReleasePath ? getURL(repoBaseURL, repoReleasePath) : repoReleaseURL
                project.logger.info('Repository [{}] is configured, therefore artifacts will be published directly.', repo)
                applyDirectPublishing(project.getRootProject(), repo, repoUserLogin, repoUserPassword)
                project.getRootProject().getSubprojects().each { Project subp ->
                    applyDirectPublishing(project.getRootProject(), repo, repoUserLogin, repoUserPassword)
                }
            } else if(repoBaseURL && repoUserLogin && repoUserPassword) {
                project.rootProject.plugins.apply(NexusStagingPlugin)
                project.logger.info('Intershop publishing will be configured for staging to {} or snapshot build {}', repoBaseURL, repoSnapshotURL)

                project.rootProject.nexusStaging {
                    repositoryDir = new File(project.getRootProject().buildDir, 'staging/repo')
                    resultPropertiesFile = new File(project.getRootProject().buildDir, 'staging/results/repotransfer.properties')
                }
                project.getRootProject().afterEvaluate {
                    project.nexusStaging.description = "release ${project.name} ${project.version}"
                }
            }

            applyPublishingConfig(project.getRootProject(), repoSnapshotURL, repoUserLogin, repoUserPassword)
            project.getRootProject().getSubprojects().each { Project subp ->
                applyPublishingConfig(subp, repoSnapshotURL, repoUserLogin, repoUserPassword)
            }

            String jiraBaseURL = getVariable(project, JIRA_BASEURL_ENV, JIRA_BASEURL_PRJ, '')
            String jiraUserLogin = getVariable(project, JIRA_USER_NAME_ENV, JIRA_USER_NAME_PRJ, '')
            String jiraUserPassword = getVariable(project, JIRA_USER_PASSWORD_ENV, JIRA_USER_PASSWORD_PRJ, '')

            if(jiraBaseURL && jiraUserLogin && jiraUserPassword) {
                project.rootProject.plugins.apply(JiraConnectorPlugin)

                project.rootProject.jiraConnector {
                    linePattern = '3\\+.*'
                    fieldName = jiraFieldName
                    versionMessage = 'version created by build plugin'
                    issueFile = project.rootProject.tasks.changelog.outputs.files.singleFile

                    if (project.name.contains('_')) {
                        fieldPattern = '[a-z1-9]*_(.*)'
                    }
                }

                project.rootProject.tasks.setIssueField.dependsOn project.rootProject.tasks.changelog

                project.getRootProject().afterEvaluate {
                    project.jiraConnector.fieldValue = "${project.name}/${project.version}"

                    if(! project.version.endsWith('-SNAPSHOT')) {
                        project.rootProject.tasks.publish.dependsOn project.tasks.setIssueField
                    }
                }
            }

            project.rootProject.rootProject.afterEvaluate {
                if(! project.version.endsWith('-SNAPSHOT')) {
                    // add javadoc to root project
                    project.getRootProject().ext.releaseWithJavaDoc = 'true'
                    // add javadoc to sub project
                    project.getRootProject().getSubprojects().each { Project subp ->
                        subp.ext.releaseWithJavaDoc = 'true'
                    }
                } else {
                    System.setProperty('ENABLE_SNAPSHOTS', 'true')
                }
            }
        }
    }

    private void applyPublishingConfig(Project p, String snapshotURL, String repoUser, String repoUserPasswd) {
        p.plugins.withType(IvyPublishPlugin) {
            p.publishing {
                repositories {
                    if (!delegate.findByName(IVYREPONAME) && p.version.endsWith('-SNAPSHOT')) {
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
                if (!delegate.findByName(IVYREPONAME) && p.version.endsWith('-SNAPSHOT')) {
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
                    if (!delegate.findByName(MVNREPOName) && p.version.endsWith('-SNAPSHOT')) {
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
                if (!delegate.findByName(MVNREPOName) && p.version.endsWith('-SNAPSHOT')) {
                    p.logger.info('Mvn repository {} added to the project repository configuration', MVNREPOName)
                    maven {
                        name MVNREPOName
                        url "${snapshotURL ?: repoURL}content/repositories/snapshots"
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

    private void applyDirectPublishing(Project p, String releaseURL, String repoUser, String repoUserPasswd) {
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
