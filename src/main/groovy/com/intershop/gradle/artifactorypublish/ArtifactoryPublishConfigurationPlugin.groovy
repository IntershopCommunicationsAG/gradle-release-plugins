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
package com.intershop.gradle.artifactorypublish

import com.intershop.gradle.jiraconnector.JiraConnectorPlugin
import com.intershop.gradle.repoconfig.RepoConfigRegistry
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin

/**
 * This is the implementation of the plugin.
 */
class ArtifactoryPublishConfigurationPlugin implements Plugin<Project> {

    // run on CI server
    public final static String RUNONCI_ENV = 'RUNONCI'
    public final static String RUNONCI_PRJ = 'runOnCI'

    /** Repository Configuration - Start **/
    // Repo SNAPSHOT URL
    public final static String SNAPSHOT_URL_ENV = 'SNAPSHOTURL'
    public final static String SNAPSHOT_URL_PRJ = 'snapshotURL'

    // Repo RELEASE URL
    public final static String RELEASE_URL_ENV = 'RELEASEURL'
    public final static String RELEASE_URL_PRJ = 'releaseURL'

    // Repo SNAPSHOT Path (based on Nexus Base URL configuration)
    public final static String SNAPSHOT_KEY_ENV = 'SNAPSHOTPATH'
    public final static String SNAPSHOT_KEY_PRJ = 'snapshotPath'

    // Repo RELEASE Path (based on Nexus Base URL configuration)
    public final static String RELEASE_KEY_ENV = 'RELEASEPATH'
    public final static String RELEASE_KEY_PRJ = 'releasePath'

    // dublicated from nexusstaging-gradle-plugin
    public final static String REPO_BASEURL_ENV = 'ARTIFACTORYBASEURL'
    public final static String REPO_BASEURL_PRJ = 'artifactoryBaseURL'

    public final static String REPO_USER_NAME_ENV = 'ARTIFACTORYUSERNAME'
    public final static String REPO_USER_NAME_PRJ = 'artifactoryUserName'

    public final static String REPO_USER_PASSWORD_ENV = 'ARTIFACTORYUSERPASSWD'
    public final static String REPO_USER_PASSWORD_PRJ = 'artifactoryUserPASSWD'

    /** Repository Configuration - End **/

    /** Jira configuration - start **/
    public final static String JIRA_BASEURL_ENV = 'JIRABASEURL'
    public final static String JIRA_BASEURL_PRJ = 'jiraBaseURL'

    public final static String JIRA_USER_NAME_ENV = 'JIRAUSERNAME'
    public final static String JIRA_USER_NAME_PRJ = 'jiraUserName'

    public final static String JIRA_USER_PASSWORD_ENV = 'JIRAUSERPASSWD'
    public final static String JIRA_USER_PASSWORD_PRJ = 'jiraUserPASSWD'

    /** Jira configuration - End **/

    public void apply(Project project) {

        String runOnCI = getVariable(project, RUNONCI_ENV, RUNONCI_PRJ, 'false')
        project.logger.info('Publishing Configuration: RunOnCI: {}', runOnCI.toBoolean())

        if (runOnCI.toBoolean()) {
            project.logger.info('Intershop release publishing configuration will be applied to project {}', project.name)

            if (!project.rootProject.tasks.findByName('changelog')) {
                throw new GradleException('Please apply also "com.intershop.gradle.scmversion"')
            }


            String jiraFieldName = project.hasProperty('jiraFieldName') ? project.property('jiraFieldName') : 'Fix Version/s'

            String repoBaseURL = getVariable(project, REPO_BASEURL_ENV, REPO_BASEURL_PRJ, '')
            String repoUserLogin = getVariable(project, REPO_USER_NAME_ENV, REPO_USER_NAME_PRJ, '')
            String repoUserPassword = getVariable(project, REPO_USER_PASSWORD_ENV, REPO_USER_PASSWORD_PRJ, '')

            String repoReleaseKey = getVariable(project, RELEASE_KEY_ENV, RELEASE_KEY_PRJ, '')
            String repoSnapshotKey = getVariable(project, SNAPSHOT_KEY_ENV, SNAPSHOT_KEY_PRJ, '')

            project.rootProject.plugins.apply(ArtifactoryPlugin)

            artifactory {
                if(repoBaseURL) {
                    contextUrl = repoBaseURL
                }
                publish {
                    repository {
                        repoKey = project.version.toString().endsWith('-SNAPSHOT') ? repoSnapshotKey : repoReleaseKey
                        username = repoUserLogin
                        password = repoUserPassword
                    }
                    ivy {
                        ivyLayout = RepoConfigRegistry.ivyPattern
                        artifactLayout = RepoConfigRegistry.artifactPattern
                    }
                }
                defaults {
                    publications ('ivy', 'maven')
                }
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

    /**
     * Calculates the setting for special configuration from the system
     * or java environment or project properties.
     *
     * @param envVar        name of environment variable
     * @param projectVar    name of project variable
     * @param defaultValue  default value
     * @return              the string configuration
     */
    private String getVariable(Project project, String envVar, String projectVar, String defaultValue) {
        if(System.properties[envVar]) {
            project.logger.debug('Specified from system property {}.', envVar)
            return System.properties[envVar].toString().trim()
        } else if(System.getenv(envVar)) {
            project.logger.debug('Specified from system environment property {}.', envVar)
            return System.getenv(envVar).toString().trim()
        } else if(project.hasProperty(projectVar) && project."${projectVar}") {
            project.logger.debug('Specified from project property {}.', projectVar)
            return project."${projectVar}".toString().trim()
        }
        return defaultValue
    }
}
