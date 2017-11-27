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

import com.intershop.gradle.buildinfo.BuildInfoExtension
import com.intershop.gradle.buildinfo.BuildInfoPlugin
import com.intershop.gradle.jiraconnector.JiraConnectorPlugin
import com.intershop.gradle.repoconfig.RepoConfigRegistry
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention

import static com.intershop.gradle.util.PluginHelper.*

/**
 * This plugin adds a configuration for an publishing with artifactory.
 * A change log will be created and Jira issues adapted. This behaviour
 * depends on the settings of environment variables.
 */
@CompileDynamic
class ArtifactoryPublishConfigurationPlugin implements Plugin<Project> {

    /** Repository Configuration - Start **/

    // Repo SNAPSHOT Path (based on Artifactory Base URL configuration)
    public final static String SNAPSHOT_KEY_ENV = 'SNAPSHOTREPOKEY'
    public final static String SNAPSHOT_KEY_PRJ = 'snapshotRepoKey'

    // Repo RELEASE Path (based on Artifactory Base URL configuration)
    public final static String RELEASE_KEY_ENV = 'RELEASEREPOKEY'
    public final static String RELEASE_KEY_PRJ = 'releaseRepoKey'

    // dublicated from artifactory-gradle-plugin
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

    void apply(Project project) {
        // apply SimpleArtifactoryPublishConfigurationPlugin
        project.rootProject.plugins.apply(SimpleArtifactoryPublishConfigurationPlugin)


        //Configuration will be applied only if runOnCI is true
        String runOnCI = getVariable(project, RUNONCI_ENV, RUNONCI_PRJ, 'false')
        project.logger.debug('Publishing Configuration: RunOnCI: {}', runOnCI.toBoolean())

        if (runOnCI.toBoolean()) {
            project.logger.info('Intershop Jira editing will be applied to project {}', project.name)

            // Check for other plugins ...
            if (!project.rootProject.tasks.findByName('changelog')) {
                throw new GradleException('Please apply also "com.intershop.gradle.scmversion"')
            }

            // Jira editor configuration
            String jiraBaseURL = getVariable(project, JIRA_BASEURL_ENV, JIRA_BASEURL_PRJ, '')
            String jiraUserLogin = getVariable(project, JIRA_USER_NAME_ENV, JIRA_USER_NAME_PRJ, '')
            String jiraUserPassword = getVariable(project, JIRA_USER_PASSWORD_ENV, JIRA_USER_PASSWORD_PRJ, '')

            if(jiraBaseURL && jiraUserLogin && jiraUserPassword) {
                project.rootProject.plugins.apply(JiraConnectorPlugin)

                String jiraFieldName = project.hasProperty('jiraFieldName') ? project.property('jiraFieldName') : 'Fix Version/s'

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

                // run change log creation with a separate call ...
                project.rootProject.tasks.maybeCreate('releaseLog')

                // configuration depends on version ... this is available after evaluation
                project.getRootProject().afterEvaluate {
                    project.jiraConnector.fieldValue = "${project.name}/${project.version}"

                    //... only if the version does not end with SNAPSHOT
                    if(! project.version.toString().endsWith('-SNAPSHOT')) {
                        project.rootProject.tasks.releaseLog.dependsOn project.tasks.setIssueField
                    }
                }
            }

            // configuration depends on version ... this is available after evaluation
            project.rootProject.afterEvaluate {
                if(project.version.toString().endsWith('-SNAPSHOT')) {
                    System.setProperty('ENABLE_SNAPSHOTS', 'true')
                }
            }

        }
    }
}
