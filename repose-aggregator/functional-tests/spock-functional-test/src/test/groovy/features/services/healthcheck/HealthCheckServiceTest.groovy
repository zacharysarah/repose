package features.services.healthcheck
import framework.ReposeValveTest
import framework.TestUtils
import org.rackspace.deproxy.Deproxy

class HealthCheckServiceTest extends ReposeValveTest{

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/badconfig", params)
        repose.start(true, false)
        TestUtils.waitUntilReadyToServiceRequests(reposeEndpoint, "503")

    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "when a bad config is loaded for dist-datastore service repose should return 503s"(){

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "GET"])

        then: "Repose should return with a 503"
        messageChain.receivedResponse.code == "503"

        and: "The request should not have reached the origin service"
        messageChain.handlings.size() == 0


    }
}
