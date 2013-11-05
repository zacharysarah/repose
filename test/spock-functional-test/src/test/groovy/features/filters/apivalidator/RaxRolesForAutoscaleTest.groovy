package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

/**
 * A test to verify that a user can validate roles via api-checker
 * by setting the enable-rax-roles attribute on a validator.
 */
class RaxRolesForAutoscaleTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/autoscale")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("#method with x-roles=#headers,expected response=#responseCode")
    def "when enable-rax-role is true, user authorized to access the entire wadl"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                         | responseCode
        "GET"    | ["x-roles": "raxRolesDisabled"] | "200"
        "PUT"    | ["x-roles": "raxRolesDisabled"] | "200"
        "POST"   | ["x-roles": "raxRolesDisabled"] | "200"
        "DELETE" | ["x-roles": "raxRolesDisabled"] | "200"
    }


}
