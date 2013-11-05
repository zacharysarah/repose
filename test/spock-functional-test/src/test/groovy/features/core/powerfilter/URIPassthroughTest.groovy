package features.core.powerfilter
import framework.ReposeValveTest
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
/**
 *
 */
class URIPassthroughTest extends ReposeValveTest {


    def setupSpec() {
        repose.applyConfigs("features/core/powerfilter/uriencoding")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "special characters not escaped in URI should be forwarded to origin service"() {

        given: "I have special characters in my request URI"
        String uri = "/users?marker=user019@%"

        when: "I send a request to REPOSE with my headers"
        HttpClient client = new DefaultHttpClient()
        def status = client.execute(new HttpGet(reposeEndpoint + uri))


        then: "I get a response of 200"
        status.statusCode == "200"
    }

    def "special characters escaped in URI should be forwarded to origin service"() {

        given: "I have special characters in my request URI"
        String uri = "/users?marker=user019%40%25"

        when: "I send a request to REPOSE with my headers"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + uri)

        then: "I get a response of 200"
        mc.receivedResponse.code == "200"

        and: "The origin service receives my full request URI"
        mc.handlings.get(0).request.path == uri
    }
}
