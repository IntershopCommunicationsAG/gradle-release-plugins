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


package com.intershop.gradle.test.util

import com.squareup.okhttp.mockwebserver.Dispatcher
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.RecordedRequest

class TestDispatcher {

    public static Dispatcher getIntegrationDispatcher(Map responses, List<String> uploadlist) {
        String[] jiraResponses = ['emptyLabels.response', 'oneversionLabels.response']
        int jiraResponseCount = 0

        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                String line = request.getRequestLine()
                String path = request.getPath()
                String userAgent = request.headers.get('User-Agent')

                if(userAgent.contains('ArtifactoryBuildClient')) {
                    MockResponse artifactoryResponse = null

                    if (path.startsWith('/releases') && path.contains('jar')) {
                        uploadlist.add(path)
                        if(path.contains('1a')) {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/releases/com.intershop.testproject/project1a/1.0.0/jars/project1a-jar-1.0.0.jar")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactoryReleaseArtifact1a.response'))
                        } else if(path.contains('2b')) {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/releases/com.intershop.testproject/project2b/1.0.0/jars/project2b-jar-1.0.0.jar")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactoryReleaseArtifact2b.response'))
                        } else {
                            if(path.contains('com.intershop')) {
                                artifactoryResponse = new MockResponse()
                                        .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                        .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                        .addHeader("Location", "http://localhost:80/releases/com.intershop/testProject/1.1.0/jars/testProject-jar-1.1.0.jar")
                                        .addHeader("Cache-Control", "no-cache")
                                        .setBody(getResponse('artifactoryReleaseArtifact.response'))
                            } else if(path.contains('com/intershop')) {
                                artifactoryResponse = new MockResponse()
                                        .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                        .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                        .addHeader("Location", "http://localhost:80/releases/com/intershop/testProject/1.1.0/jars/testProject-jar-1.1.0.jar")
                                        .addHeader("Cache-Control", "no-cache")
                                        .setBody(getResponse('artifactoryReleaseArtifactMaven.response'))
                            }
                        }
                        return artifactoryResponse
                    }
                    if (path.startsWith('/releases') && (path.contains('ivy') || path.contains('pom'))) {
                        uploadlist.add(path)
                        if(path.contains('1a')) {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/releases/com.intershop.testproject/project1a/1.0.0/ivys/ivy-1.0.0.xml")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactoryReleaseIvy1a.response'))
                        } else if(path.contains('2b')) {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/releases/com.intershop.testproject/project2b/1.0.0/ivys/ivy-1.0.0.xml")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactoryReleaseIvy2b.response'))
                        } else {
                            if(path.contains('com.intershop')) {
                                artifactoryResponse = new MockResponse()
                                        .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                        .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                        .addHeader("Location", "http://localhost:80/releases/com.intershop/testProject/1.1.0/ivys/ivy-1.1.0.xml")
                                        .addHeader("Cache-Control", "no-cache")
                                        .setBody(getResponse('artifactoryReleaseIvy.response'))
                            } else if(path.contains('com/intershop')) {
                                artifactoryResponse = new MockResponse()
                                        .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                        .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                        .addHeader("Location", "http://localhost:80/artifactory/releases/com/intershop/testProject/1.1.0/testProject-1.1.0.pom")
                                        .addHeader("Cache-Control", "no-cache")
                                        .setBody(getResponse('artifactoryReleasePomMaven.response'))
                            }
                        }
                        return artifactoryResponse
                    }
                    if (path.startsWith('/snapshots') && path.contains('jars')) {
                        uploadlist.add(path)
                        if(path.contains('1a')) {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/snapshots/com.intershop.testproject/project1a/1.0.0-SNAPSHOT/jars/project1a-jar-1.0.0-SNAPSHOT.jar")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactorySnapshotArtifact1a.response'))
                        } else if(path.contains('2b')) {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/snapshots/com.intershop.testproject/project2b/1.0.0-SNAPSHOT/jars/project2b-jar-1.0.0-SNAPSHOT.jar")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactorySnapshotArtifact2b.response'))
                        } else {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/snapshots/com.intershop/testProject/1.0.0-SNAPSHOT/jars/testProject-jar-1.0.0-SNAPSHOT.jar")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactorySnapshotArtifact.response'))
                        }
                        return artifactoryResponse
                    }
                    if (path.startsWith('/snapshots') && path.contains('ivys')) {
                        uploadlist.add(path)
                        if(path.contains('1a')) {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/snapshots/com.intershop.testproject/project1a/1.0.0-SNAPSHOT/ivys/ivy-1.0.0-SNAPSHOT.xml")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactorySnapshotIvy1a.response'))
                        } else if(path.contains('2b')) {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/snapshots/com.intershop.testproject/project2b/1.0.0-SNAPSHOT/ivys/ivy-1.0.0-SNAPSHOT.xml")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactorySnapshotIvy2b.response'))
                        } else {
                            artifactoryResponse = new MockResponse()
                                    .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.ItemCreated+json;charset=ISO-8859-1")
                                    .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                    .addHeader("Location", "http://localhost:80/snapshots/com.intershop/testProject/1.0.0-SNAPSHOT/ivys/ivy-1.0.0-SNAPSHOT.xml")
                                    .addHeader("Cache-Control", "no-cache")
                                    .setBody(getResponse('artifactorySnapshotIvy.response'))
                        }
                        return artifactoryResponse
                    }
                    if (path.startsWith('/api/system/version')) {
                        artifactoryResponse = new MockResponse()
                                .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.system.Version+json")
                                .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                .addHeader("Cache-Control", "no-cache")
                                .setBody(getResponse('artifactoryApiVersion.response'))
                        return artifactoryResponse
                    }
                    if (path.startsWith('/api/build')) {
                        artifactoryResponse = new MockResponse()
                                .addHeader("Content-Type", "application/vnd.org.jfrog.artifactory.system.Version+json")
                                .addHeader("X-Artifactory-Id", "c2ba7b5d6d0ce49c:-6509cd21:157dd63986a:-8000")
                                .addHeader("Cache-Control", "no-cache")
                                .setResponseCode(204)
                        return artifactoryResponse
                    }
                }
                if(path.startsWith('/nexus/service/local/staging/profile_evaluate')) {
                    MockResponse nexus_response = new MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(getResponse('profile_evaluate.response'))
                    return nexus_response
                }
                if(path.startsWith('/nexus/service/local/staging/profile_repositories/19124894924ac')) {
                    MockResponse nexus_response = new MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(getResponse('profile_repositories.response'))
                    return nexus_response
                }
                if(path.startsWith('/nexus/service/local/staging/bulk/close')){
                    MockResponse close_response = new MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .addHeader("Cache-Control", "no-cache")

                    return close_response
                }
                if(path.startsWith('/nexus')) {
                    uploadlist.add(path)
                }
                if(line.startsWith('GET /rest/api/latest/issue/ISTOOLS-993?expand=schema,names,transitions')) {
                    MockResponse issue_response = new MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(getResponse(jiraResponses[jiraResponseCount]))
                    jiraResponseCount =+ 1
                    return issue_response
                }
                if(line.startsWith('GET /rest/api/latest/field')) {
                    MockResponse field_response = new MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .addHeader("Cache-Control", "no-cache")
                            .setBody(getResponse('fieldLabels.response'))
                    return field_response
                }
                if(line.startsWith('PUT /rest/api/latest/issue/ISTOOLS-993')) {
                    responses.put('onebody', request.getBody().readUtf8().toString())
                    MockResponse close_response = new MockResponse()
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .addHeader("Cache-Control", "no-cache")
                    return close_response
                }

                return new MockResponse()
            }
        }
    }

    private static String getResponse(String name) {
        ClassLoader classLoader = TestDispatcher.class.getClassLoader();
        URL resource = classLoader.getResource(name);
        if (resource == null) {
            throw new RuntimeException("Could not find classpath resource: $name")
        }

        File resourceFile = new File(resource.toURI())
        return resourceFile.text.stripIndent()
    }
}
