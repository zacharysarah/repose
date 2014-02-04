package features.filters.ratelimiting

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 2/4/14
 * Time: 4:12 PM
 */
class LimitKeysTest extends Specification {
    Deproxy deproxy

    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose

    def setup() {

        properties = new TestProperties()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        reposeConfigProvider = new ReposeConfigurationProvider(properties.configDirectory, properties.configTemplates)

        def params = properties.getDefaultTemplateParams()
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/filters/ratelimiting/limitkeys", params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.reposeJar,
                properties.reposeEndpoint,
                properties.configDirectory,
                properties.reposePort,
                properties.reposeShutdownPort
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(properties.reposeEndpoint)
    }

    def cleanup() {

        if (repose) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }

    def "Limits with the same uri and regex should not step on each other"() {

        given:
        def mc
        String url = "${properties.reposeEndpoint}/foo/resource"
        def headers = ['X-PP-User': 'user', 'X-PP-Groups': 'this-group']


        when: "we make a get request"
        mc = deproxy.makeRequest(method: "GET", url: url, headers: headers)

        then: "it should return a 200"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a post request"
        mc = deproxy.makeRequest(method: "POST", url: url, headers: headers)

        then: "it should return a 200"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make another get request"
        mc = deproxy.makeRequest(method: "GET", url: url, headers: headers)

        then: "it should return a 200"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }
}
