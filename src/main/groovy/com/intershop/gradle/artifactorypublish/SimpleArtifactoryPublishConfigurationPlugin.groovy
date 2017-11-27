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
import com.intershop.gradle.repoconfig.RepoConfigRegistry
import groovy.transform.CompileDynamic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention

import static com.intershop.gradle.util.PluginHelper.*
/**
 * This plugin adds a configuration for an publishing with artifactory only.
 */
@CompileDynamic
class SimpleArtifactoryPublishConfigurationPlugin implements Plugin<Project> {

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

    void apply(Project project) {
        // apply build info plugin - necessary for Artifactory properties
        project.rootProject.plugins.apply(BuildInfoPlugin)
        // apply Artifactory publishing plugin
        project.rootProject.plugins.apply(ArtifactoryPlugin)

        //Configuration will be applied only if runOnCI is true
        String runOnCI = getVariable(project, RUNONCI_ENV, RUNONCI_PRJ, 'false')
        project.logger.debug('Publishing Configuration: RunOnCI: {}', runOnCI.toBoolean())

        if (runOnCI.toBoolean()) {
            project.logger.info('Intershop publishing configuration for Artifactory will be applied to project {}', project.name)

            // Publishing configuration
            String repoBaseURL = getVariable(project, REPO_BASEURL_ENV, REPO_BASEURL_PRJ, '')
            String repoUserLogin = getVariable(project, REPO_USER_NAME_ENV, REPO_USER_NAME_PRJ, '')
            String repoUserPassword = getVariable(project, REPO_USER_PASSWORD_ENV, REPO_USER_PASSWORD_PRJ, '')

            String repoReleaseKey = getVariable(project, RELEASE_KEY_ENV, RELEASE_KEY_PRJ, '')
            String repoSnapshotKey = getVariable(project, SNAPSHOT_KEY_ENV, SNAPSHOT_KEY_PRJ, '')

            if(repoBaseURL && repoUserLogin && repoUserPassword && repoReleaseKey && repoSnapshotKey) {
                ArtifactoryPluginConvention artifactoryPluginConvention = project.rootProject.convention.getPlugin(ArtifactoryPluginConvention)
                BuildInfoExtension infoExtension = project.extensions.findByType(BuildInfoExtension)

                project.artifactory {
                    contextUrl = repoBaseURL
                    publish {
                        repository {
                            username = repoUserLogin
                            password = repoUserPassword

                            ivy {
                                ivyLayout = RepoConfigRegistry.ivyPattern
                                artifactLayout = RepoConfigRegistry.artifactPattern
                                mavenCompatible = false
                            }
                        }
                    }

                    String buildNumber = infoExtension.ciProvider.buildNumber?:'' + new Random(System.currentTimeMillis()).nextInt(20000)
                    String buildTimeStamp = infoExtension.ciProvider.buildTime?:'' + (new Date()).toTimestamp()
                    String vcsRevision = infoExtension.scmProvider.SCMRevInfo?:'unknown'

                    clientConfig.info.setBuildName(infoExtension.ciProvider.buildJob?:project.name)
                    clientConfig.info.setBuildNumber(buildNumber)
                    clientConfig.info.setBuildTimestamp(buildTimeStamp)
                    clientConfig.info.setBuildUrl(infoExtension.ciProvider.buildUrl?:'unknown')
                    clientConfig.info.setVcsRevision(vcsRevision)
                    clientConfig.info.setVcsUrl(infoExtension.scmProvider.SCMOrigin?:'unknown')

                    clientConfig.publisher.addMatrixParam('build.number', buildNumber)
                    clientConfig.publisher.addMatrixParam('vcs.revision', vcsRevision)
                    clientConfig.publisher.addMatrixParam('build.timestamp', buildTimeStamp)

                    clientConfig.publisher.addMatrixParam('build.java.version', infoExtension.infoProvider.javaVersion)
                    clientConfig.publisher.addMatrixParam('source.java.version', infoExtension.infoProvider.javaSourceCompatibility ?: infoExtension.infoProvider.javaVersion.split('_')[0])
                    clientConfig.publisher.addMatrixParam('target.java.version', infoExtension?.infoProvider.javaTargetCompatibility ?: infoExtension?.infoProvider.javaVersion.split('_')[0])
                    clientConfig.publisher.addMatrixParam('build.status', infoExtension?.infoProvider.projectStatus?:'unknown')
                    clientConfig.publisher.addMatrixParam('build.date', infoExtension?.infoProvider.OSTime?:'unknown')
                    clientConfig.publisher.addMatrixParam('gradle.version', infoExtension?.infoProvider.gradleVersion?:'unknown')
                    clientConfig.publisher.addMatrixParam('gradle.rootproject', infoExtension?.infoProvider.rootProject?:'unknown')
                    clientConfig.publisher.addMatrixParam('scm.type', infoExtension?.scmProvider.SCMType?:'unknown')
                    clientConfig.publisher.addMatrixParam('scm.branch.name', infoExtension?.scmProvider.branchName?:'unknown')
                    clientConfig.publisher.addMatrixParam('scm.change.time', infoExtension?.scmProvider.lastChangeTime?:'unknown')

                    clientConfig.publisher.addMatrixParam('project.version', infoExtension?.infoProvider.projectVersion)
                    clientConfig.publisher.addMatrixParam('project.name', infoExtension?.infoProvider.rootProject)
                }
                project.rootProject.allprojects {
                    it.plugins.apply(ArtifactoryPlugin)
                }

                // configuration depends on version ... this is available after evaluation
                project.rootProject.afterEvaluate {
                    artifactoryPluginConvention.clientConfig.publisher.repoKey = project.version.toString().endsWith('-SNAPSHOT') ? repoSnapshotKey : repoReleaseKey
                }
            }
        }
    }
}
