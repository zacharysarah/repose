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
package features.filters.clientauthn.nogroups

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class TenantedNonDelegableNoGroupsTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    def static headersCommon = [
            'X-Default-Region': 'the-default-region',
            'x-auth-token'    : 'token',
            'x-forwarded-for' : '127.0.0.1',
            'x-pp-user'       : 'username;q=1.0'
    ]

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/nogroups", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)
    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }

    /**
     * this tests the negative scenarios in tenanted and non-delegable mode with no groups
     * - token validation fails (500)
     * - tenant id in the request does not match tenant id in the response from identity and service admin role is not present
     * - token is not found (404)
     * - get groups call responds with 500
     * - get groups call returns with a 404
     * - token returns expired
     * @return
     */
    @Unroll("For request tenant: #requestTenant, identity returns #authResponseCode, groups response is #groupResponseCode with response tenant #responseTenant")
    def "when authenticating user in tenanted and non delegable mode - fail scenarios"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            service_admin_role = "not-admin"
        }

        if (authResponseCode != 200) {
            fakeIdentityService.validateTokenHandler = {
                tokenId, request, xml ->
                    new Response(authResponseCode)
            }
        }

        if (groupResponseCode != 200) {
            fakeIdentityService.getGroupsHandler = {
                userId, request, xml ->
                    new Response(groupResponseCode)
            }
        }

        when:
        "User passes a request through repose with request tenant: $requestTenant, response tenant: $responseTenant"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ] + headersCommon
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("www-authenticate") == x_www_auth

        where:
        requestTenant | responseTenant | authResponseCode | responseCode | groupResponseCode | x_www_auth
        113           | 113            | 500              | "500"        | 200               | false
        114           | 114            | 404              | "401"        | 200               | true
        115           | 115            | 200              | "200"        | 404               | false
        116           | 116            | 200              | "200"        | 500               | false
        111           | 112            | 200              | "401"        | 200               | true
    }

    /**
     * this tests the negative scenarios in tenanted and non-delegable mode with no groups
     * - token validation fails (500)
     * - tenant id in the request does not match tenant id in the response from identity and service admin role is not present
     * - token is not found (404)
     * - get groups call responds with 500
     * - get groups call returns with a 404
     * - token returns expired
     * @return
     */
    @Unroll("For request tenant: #requestTenant, identity returns role #serviceAdminRole with response tenant #responseTenant")
    def "when authenticating user in tenanted and non delegable mode - success"() {

        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            service_admin_role = serviceAdminRole
        }

        when:
        "User passes a request through repose with request tenant: $requestTenant, response tenant: $responseTenant in service admin role = $serviceAdminRole"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ] + headersCommon
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        request2.headers.getFirstValue("x-tenant-name") == responseTenant.toString()
        request2.headers.contains("x-token-expires")
        request2.headers.getFirstValue("x-pp-user") == "username;q=1.0"
        request2.headers.contains("x-roles")
        request2.headers.getFirstValue("x-authorization") == "Proxy $requestTenant"
        request2.headers.getFirstValue("x-user-name") == "username"
        !request2.headers.contains("x-pp-groups")

        mc.receivedResponse.headers.contains("www-authenticate") == false

        where:
        requestTenant | responseTenant | serviceAdminRole      | responseCode
        117           | 117            | "not-admin"           | "200"
        118           | 119            | "service:admin-role1" | "200"
    }
}
