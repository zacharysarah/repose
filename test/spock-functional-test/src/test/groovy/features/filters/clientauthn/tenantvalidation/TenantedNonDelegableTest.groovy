package features.filters.clientauthn.tenantvalidation

import features.filters.clientauthn.IdentityServiceRemoveTenantedValidationResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

class TenantedNonDelegableTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static IdentityServiceRemoveTenantedValidationResponseSimulator fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/tenantednondelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(), 'origin service')
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    @Unroll("Tenant: #reqTenant")
    def "when authenticating user in tenanted and non delegable mode"() {

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed
        fakeIdentityService.adminOk = isAdminAuthed

        when: "User passes a request through repose with tenant in service admin role = " + tenantWithAdminRole + " and tenant returned equal = " + tenantMatch
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        fakeIdentityService.client_userid = reqTenant
        fakeIdentityService.isValidateClientTokenBroken = validateClientBroken
        fakeIdentityService.isGetAdminTokenBroken = getAdminTokenBroken
        fakeIdentityService.isGetGroupsBroken = getGroupsBroken
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        //mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        if (mc.handlings.size() > 0) {
            mc.handlings[0].endpoint == originEndpoint
            def request2 = mc.handlings[0].request
            request2.headers.contains("X-Default-Region")
            request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        mc.receivedResponse.headers.contains("www-authenticate") == www_auth_header

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == cachedHandlings
        if (mc.handlings.size() > 0) {
            mc.handlings[0].endpoint == originEndpoint
            mc.handlings[0].request.headers.contains("X-Default-Region")
            mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | isAdminAuthed | responseCode | handlings | orphanedHandlings | cachedOrphanedHandlings | cachedHandlings | www_auth_header | validateClientBroken | getAdminTokenBroken | getGroupsBroken
        111       | false       | false               | true     | false         | "500"        | 0         | 1                 | 1                       | 0               | false           | false                | false               | false
        888       | true        | true                | true     | true          | "500"        | 0         | 1                 | 1                       | 0               | false           | false                | true                | false
        222       | true        | true                | true     | true          | "200"        | 1         | 3                 | 0                       | 1               | false           | false                | false               | false
        333       | true        | false               | true     | true          | "200"        | 1         | 2                 | 0                       | 1               | false           | false                | false               | false
        444       | false       | true                | true     | true          | "200"        | 1         | 2                 | 1                       | 1               | false           | false                | false               | false
        555       | false       | false               | true     | true          | "401"        | 0         | 1                 | 1                       | 0               | true            | false                | false               | false
        666       | false       | false               | false    | true          | "401"        | 0         | 1                 | 1                       | 0               | true            | false                | false               | false
        777       | true        | true                | true     | true          | "500"        | 0         | 1                 | 1                       | 0               | false           | true                 | false               | false
        100       | true        | true                | true     | true          | "500"        | 1         | 2                 | 1                       | 0               | false           | false                | false               | true
    }


}
