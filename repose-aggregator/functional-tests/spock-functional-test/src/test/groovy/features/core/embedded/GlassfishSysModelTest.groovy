package features.core.embedded

import framework.*
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import spock.lang.Shared
import spock.lang.Specification

class GlassfishSysModelTest extends Specification {
    static Deproxy deproxy
    static ReposeLauncher repose
    static String reposeGlassfishEndpoint
    static def params

    @Shared
    def sout = new StringBuffer()
    @Shared
    def serr = new StringBuffer()

    @Shared
    def ReposeLogSearch logSearch

    def setupSpec() {
        //def reposeClusterId = "repose"
        //def reposeNodeId = "node"
        //def reposeNodeHostname = "localhost"

        def TestProperties properties = new TestProperties()
        def logFile = properties.logFile
        // get ports
        int originServicePort = properties.targetPort

        println("Deproxy: " + originServicePort)
        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)


        int reposePort = properties.reposePort
        int shutdownPort = properties.reposeShutdownPort

        println("repose: ${reposePort}")

        // configure and start repose

        reposeGlassfishEndpoint = "http://localhost:${reposePort}"

        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()

        params = properties.getDefaultTemplateParams()
        params += [
                'reposePort'          : reposePort,
                'repose.cluster.id'   : "repose",
                'repose.node.id'      : 'node1',
                'repose.hostname'     : 'localhost'
        ]

        ReposeConfigurationProvider config1 = new ReposeConfigurationProvider(configDirectory, configTemplates)

        config1.applyConfigs("features/core/embedded/badhost", params)
        config1.applyConfigs("common", params)

        repose = new ReposeContainerLauncher(config1, properties.getGlassfishJar(), "repose1", "node1", rootWar, reposePort, shutdownPort)
        logSearch = new ReposeLogSearch(logFile);

        repose.start()
        repose.process.consumeProcessOutput(sout, serr)
        repose.process.waitForOrKill(5000)
        repose.waitForNon500FromUrl(reposeGlassfishEndpoint, 10)
    }

    def cleanupSpec() {
        println sout
        println serr
        if(repose)
            repose.stop()
        if(deproxy)
            deproxy.shutdown()
    }

    def "when the host cannot find itself in the system model, then Repose should not crash"() {
        when:
        MessageChain mc = deproxy.makeRequest(
                   url: reposeGlassfishEndpoint + "/cluster", headers: ['x-trace-request': 'true','x-pp-user':'usertest1'])

        then:
        logSearch.searchByString("NullPointerException").size() == 0
    }
}
