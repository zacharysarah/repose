package features.core.configloadingandreloading

import framework.ReposeValveTest
import framework.category.Bug
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy

import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class LoggingPropertiesTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def setup() {
        repose.configurationProvider.cleanConfigDirectory()
    }

    def cleanupSpec() {
        if (repose?.isUp()) {
            repose.stop(30000, false)
        }
        deproxy?.shutdown()
    }

    /**
     * Note: Due to the halting problem, it is not possible to prove whether or not Repose will hang on certain input.
     *       This test bounds Repose's execution time to give some indication of whether or not things are working
     *       as expected.
     */
    @Category([Bug, Slow])
    def "if no properties file is provided, Repose should not hang due to the log buffer being full"() {
        given:
        def params = properties.defaultTemplateParams
        def hitRepose = new Runnable() {
            @Override
            void run() {
                deproxy.makeRequest(reposeEndpoint)
            }
        }

        repose.configurationProvider.applyConfigs("features/core/startup/logging/nofilespecified", params)

        when:
        repose.start(true, false)
        repose.waitForDesiredResponseCodeFromUrl(reposeEndpoint, 200, 60, 5) // Note that this line does not hang even though it adds to the log
        10.times { // Make some number of requests to populate the log
            def future = new FutureTask<Object>(hitRepose, null)
            future.get(30, TimeUnit.SECONDS) // If no response after 30 seconds, throw a TimeoutException
        }

        then:
        notThrown(TimeoutException)
    }
}
