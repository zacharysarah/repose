package features.services.httpconnectionpool

import framework.ReposeValveTest
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handlers
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response


class HttpClientServiceTest extends ReposeValveTest {

    def setup() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose) {
            try {
                repose.stop()
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    def "repose should use http conn pool service for origin service"() {
        given: "Repose is started with a default connection pool"
        repose.applyConfigs("features/services/httpconnectionpool/common",
                "features/services/httpconnectionpool/defaultpool")
        repose.start()
        waitUntilReadyToServiceRequests()
        List<Thread> clientThreads = new ArrayList<Thread>()
        List<Handling> handlings = Collections.synchronizedList(new ArrayList<Handling>());

        when: "Lots of clients make lots of calls"
        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/")
                    Handling handling = mc.handlings.get(0)
                    handlings.add(handling)
                }
            }
            clientThreads.add(thread)
        }

        clientThreads*.join()

        then: "The total connections to the origin service should be numClients * callsPerClient"
        handlings.connection.size() == numClients * callsPerClient

        then: "The total unique connections established on the origin service side should be maxPerRoute: 2"
        handlings.connection.unique().size() <= 2

        then: "All responses should be handled as expected with a 200"
        handlings.response.code.unique().size() == 1
        handlings.response.code.unique().get(0) == "200"

        where:
        numClients | callsPerClient
        10         | 10
    }

    def "repose completes the handling of inflight connections when reconfiguring CP service"() {
        given: "repose is started with default connection pooling"
        repose.applyConfigs("features/services/httpconnectionpool/common")
        repose.start()
        waitUntilReadyToServiceRequests()

        when: "a request is made that takes some amount of time"
        def MessageChain slowThreadMC
        Thread slowResponseThread = Thread.start {

            slowThreadMC = deproxy.makeRequest(
                    url: reposeEndpoint + "/",
                    defaultHandler: Handlers.Delay(8000),
                    headers: ['x-trace-request': 'true'])
        }

        and: "the connection pool is reconfigured"
        repose.updateConfigs("features/services/httpconnectionpool/defaultpool")
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/")

        and: "I wait for the slow request to finish"
        slowResponseThread.join()

        then: "Both requests should be successful"
        slowThreadMC != null
        slowThreadMC.getHandlings().size() > 0
        slowThreadMC.getHandlings().get(0).response.code == "200"
        mc.handlings.get(0).response.code == "200"
    }

    def "ridiculous number of connection pool reconfigures won't phase the ConnectionPoolService"() {
        given: "repose is started with default connection pooling"
        repose.applyConfigs("features/services/httpconnectionpool/common", "features/services/httpconnectionpool/defaultpool")
        repose.start()
        waitUntilReadyToServiceRequests()
        List<Thread> clientThreads = new ArrayList<Thread>()
        List<Handling> handlings = Collections.synchronizedList(new ArrayList<Handling>());

        when: "Alot of clients connect to repose and start issuing requests"
        for (x in 1..numClients) {

            def thread = Thread.stÔart {
                for (i in 1..callsPerClient) {
                    MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/")
                    Handling handling = mc.handlings.get(0)
                    handlings.add(handling)
                }
            }
            clientThreads.add(thread)
        }

        and: "Repose is reconfigured many times"
        for (x in 1..10) {
            if (x % 2) {
                repose.updateConfigs(3, "features/services/httpconnectionpool/defaultpool")
            } else {
                repose.updateConfigs(5, "features/services/httpconnectionpool/custompool")
            }
        }

        and: "All clients have completed their requests"
        clientThreads*.join()

        then: "The total connections to the origin service should be numClients * callsPerClient"
        handlings.connection.size() == numClients * callsPerClient

        then: "All responses should be handled as expected with a 200"
        handlings.response.code.unique().size() == 1
        handlings.response.code.unique().get(0) == "200"

        where:
        numClients | callsPerClient
        20         | 40
    }


    def "shutting down repose should release all connections"() {}

//    def "repose should use http conn pool service for DD service"() {
//        given: "repose is started with default connection pooling"
//        repose.applyConfigs("features/services/httpconnectionpool/withddservice")
//        repose.start()
//        waitUntilReadyToServiceRequests()
//
//
//        List<Thread> clientThreads = new ArrayList<Thread>()
//        List<Handling> handlings = Collections.synchronizedList(new ArrayList<Handling>());
//
//        when: "Alot of clients connect to repose and start issuing requests"
//        for (x in 1..numClients) {
//
//            def thread = Thread.start {
//                for (i in 1..callsPerClient) {
//                    MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/")
//                    Handling handling = mc.handlings.get(0)
//                    handlings.add(handling)
//                }
//            }
//            clientThreads.add(thread)
//        }
//
//
//
//    }

}
