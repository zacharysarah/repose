package features.filters.clientauthn.connectionpooling

import features.filters.clientauthn.IdentityServiceRemoveTenantedValidationResponseSimulator
import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.DeproxyEndpoint
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: izrik
 *
 */
class AuthNConnectionPoolingTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    IdentityServiceResponseSimulator identityService

    def setup() {

        repose.applyConfigs("common",
                "features/filters/clientauthn/connectionpooling",
                "features/filters/clientauthn/connectionpooling2")
        repose.start()

        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(), 'origin service')
        identityService = new IdentityServiceResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, identityService.handler)
    }

    def "when a client makes requests, Repose should re-use the connection to the Identity service"() {

        setup: "craft an url to a resource that requires authentication"
        def url = "${reposeEndpoint}/servers/tenantid/resource"


        when: "making two authenticated requests to Repose"
        identityService.client_token = "token1"
        identityService.client_userid = "token1"
        identityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        def mc1 = deproxy.makeRequest(url: url, headers: ['X-Auth-Token': 'token1'])
        identityService.client_token = "token2"
        identityService.client_userid = "token2"

        identityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        def mc2 = deproxy.makeRequest(url: url, headers: ['X-Auth-Token': 'token2'])
        // collect all of the handlings that make it to the identity endpoint into one list
        def allOrphanedHandlings = mc1.orphanedHandlings + mc2.orphanedHandlings
        List<Handling> identityHandlings = allOrphanedHandlings.findAll { it.endpoint == identityEndpoint }


        then: "the connections for Repose's request to Identity should have the same id"

        mc1.orphanedHandlings.size() > 0
        mc2.orphanedHandlings.size() > 0
        identityHandlings.size() > 0
        // there should be no requests to auth with a different connection id
        identityHandlings.count { it.connection != identityHandlings[0].connection } == 0
    }

    def cleanup() {

        if (repose && repose.isUp()) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
