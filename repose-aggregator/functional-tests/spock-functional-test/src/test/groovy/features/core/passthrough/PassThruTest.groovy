package features.core.passthrough

import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.category.Bug
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response
import spock.lang.Shared
import spock.lang.Unroll

class PassThruTest extends ReposeValveTest {
    def static reposeLogs

    def setupSpec() {
        reposeLogs = [
                'debug' : new ReposeLogSearch("${properties.logFile}-debug"),
                'info' : new ReposeLogSearch("${properties.logFile}-info"),
                'warn' : new ReposeLogSearch("${properties.logFile}-warn"),
                'error' : new ReposeLogSearch("${properties.logFile}-error")
        ]
        reposeLogs.each {
            k,v ->
                v.clearLog()
        }

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/passthrough/log4j", params)
        repose.start(waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(properties.reposeEndpoint)
    }

    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }

    def "passthrough request"() {
        when: "client passes a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint)

        then: "repose should respond with valid code"
        messageChain.receivedResponse.code == "200"

        and: "error logs have only ERROR messages and above"
        reposeLogs['error'].searchByString("DEBUG").size() == 0
        reposeLogs['error'].searchByString("WARN").size() == 0
        reposeLogs['error'].searchByString("INFO").size() == 0
        reposeLogs['error'].searchByString("ERROR").size() > 0

        and: "error logs have only WARN messages and above"
        reposeLogs['warn'].searchByString("DEBUG").size() == 0
        reposeLogs['warn'].searchByString("WARN").size() > 0
        reposeLogs['warn'].searchByString("INFO").size() == 0
        reposeLogs['warn'].searchByString("ERROR").size() > 0

        and: "error logs have only INFO messages and above"
        reposeLogs['info'].searchByString("DEBUG").size() == 0
        reposeLogs['info'].searchByString("WARN").size() > 0
        reposeLogs['info'].searchByString("INFO").size() > 0
        reposeLogs['info'].searchByString("ERROR").size() > 0

        and: "error logs have DEBUG messages and above"
        reposeLogs['debug'].searchByString("DEBUG").size() > 0
        reposeLogs['debug'].searchByString("WARN").size() > 0
        reposeLogs['debug'].searchByString("INFO").size() > 0
        reposeLogs['debug'].searchByString("ERROR").size() > 0

    }
}
