package features.core.embedded

import framework.*
import org.rackspace.deproxy.Deproxy
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

@Ignore
class GlassfishSysModelTest extends Specification {
    static Deproxy deproxy
    static ReposeLauncher repose
    static String reposeGlassfishEndpoint

    @Shared
    def ReposeLogSearch logSearch

    def setupSpec() {
        def reposeClusterId = "repose"
        def reposeNodeId = "node"
        def reposeNodeHostname = "localhost"

        def TestProperties properties = new TestProperties()

        def configDirectory = properties.configDirectory
        def configTemplates = properties.rawConfigDirectory
        def rootWar = properties.reposeRootWar
        def glassfishJar = properties.glassfishJar
        def reposePort = properties.reposePort
        def reposeShutdownPort = properties.reposeShutdownPort
        def logFile = properties.logFile

        def params = properties.defaultTemplateParams
        params += [
                "repose.cluster.id": reposeClusterId,
                "repose.node.id"   : reposeNodeId,
                "repose.hostname"  : reposeNodeHostname
        ]

        reposeGlassfishEndpoint = "http://localhost:${reposePort}/"

        deproxy = new Deproxy()
        deproxy.addEndpoint(reposePort)

        logSearch = new ReposeLogSearch(logFile)

        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, configTemplates)

        config.applyConfigs("common", params)
        config.applyConfigs("features/core/embedded/badhost", params)

        repose = new ReposeContainerLauncher(config, glassfishJar, reposeClusterId, reposeNodeId, rootWar,
                reposePort, reposeShutdownPort)
    }

    def cleanupSpec() {
        repose?.stop()
        deproxy?.shutdown()
    }

    def "when the host cannot find itself in the system model, then Repose should not crash"() {
        when:
        repose.start()
        repose.waitForNon500FromUrl(reposeGlassfishEndpoint, 120)

        then:
        logSearch.searchByString("NullPointerException").size() == 0
    }
}
