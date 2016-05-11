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

                //GET /nexusnexus/service/local/staging/profile_evaluate?t=maven2&g=com.intershop.project&a=project1a&v=1.0.0

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
