package features.filters.clientauthn.burst

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

class HttpConnectionPoolBurstTest extends ReposeValveTest {

    def static originEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)


        repose.configurationProvider.applyConfigs("common", properties.defaultTemplateParams)
        repose.configurationProvider.applyConfigs("features/core/passthrough/connectionpooling", properties.defaultTemplateParams)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
    }


    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }





    def "under heavy load should not drop validate token response"() {

        given:

        List<Thread> clientThreads = new ArrayList<Thread>()
        (1..numClients).each {
            threadNum ->
                def thread = Thread.start {
                    (1..callsPerClient).each {
                        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')
                    }
                }
                clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        true

        where:
        numClients | callsPerClient
        10         | 5

    }

}
