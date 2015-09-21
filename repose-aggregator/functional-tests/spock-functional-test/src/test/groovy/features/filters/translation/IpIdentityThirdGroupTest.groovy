/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

class IpIdentityThirdGroupTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/ipidentitythirdgroup", params)
        repose.start()
    }

    def "replace X-PP-Groups when ip matches"() {
        given:
        def headers = ["x-pp-user": "127.0.0.1;q=0.1", "x-pp-groups": "ip_standard;q=0.1"]

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then:
        messageChain.handlings[0].getRequest().getHeaders().getFirstValue("x-pp-groups") == "ip_super_duper;q=0.1"
    }

    def "doesn't change X-PP-Groups when ip doesn't match"() {
        given:
        def headers = ["x-pp-user": "1.2.3.4;q=0.1", "x-pp-groups": "ip_standard;q=0.1"]

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then:
        messageChain.handlings[0].getRequest().getHeaders().getFirstValue("x-pp-groups") == "ip_standard;q=0.1"
    }
}
