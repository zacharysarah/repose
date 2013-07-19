package features.filters.translation

import framework.ReposeValveTest
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.HeaderCollection
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Request
import org.rackspace.gdeproxy.Response
import org.rackspace.gdeproxy.http.SimpleHttpClient

class MissingHeadersTest extends ReposeValveTest {

    //Start repose once for this particular translation test
    def setupSpec() {
        repose.applyConfigs("features/filters/translation/missingHeaders")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        def missingHeaderErrorHandler = { Request request ->
            def headers = request.getHeaders()

            if (!headers.contains("x-rax-roles") || !headers.contains("x-rax-tenants") || !headers.contains("x-rax-username")) {
                return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING HEADERS")
            }

            return new Response(200, "OK")
        }

        deproxy._defaultHandler = missingHeaderErrorHandler

        Thread.sleep(10000)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "under heavy load should not drop headers"() {

        given:
        def PP_USER = "wizard.rocks"
        def TENANT_NAME = "Wizard Rocks"
        def ROLES = "foo bar"
        List<Thread> clientThreads = new ArrayList<Thread>()

        def missingHeader = false
        List<String> badRequests = new ArrayList()

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    def HttpClient client = new DefaultHttpClient()

                    HttpGet httpGet = new HttpGet(reposeEndpoint)
                    httpGet.addHeader('x-pp-user','lisa.rocks')
                    httpGet.addHeader('x-tenant-name', 'Lisa Rocks')
                    httpGet.addHeader('x-roles','admin observer')
                    httpGet.addHeader('thread-name', 'spock-thread-'+threadNum+'-request-'+i)

                    HttpResponse response = client.execute(httpGet)
                    if (response.getStatusLine().getStatusCode() == 500) {
                        missingHeader = true
                        badRequests.add('spock-thread-'+threadNum+'-request-'+i)
                        break
                    }
                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        missingHeader == false

        where:
        numClients | callsPerClient
        200 | 100
    }

}