package features.filters.headerNormalization

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification

class HeaderNormalizationJMXTest extends ReposeValveTest {

    String PREFIX = "\"repose-config-test-com.rackspace.papi.filters\":type=\"HeaderNormalization\",scope=\"header-normalization\""

    String HEADER_NORMALIZATION_ROOT_GET = "${PREFIX},name=\".\\*_GET\""
    String HEADER_NORMALIZATION_ROOT_POST = "${PREFIX},name=\".\\*_POST\""
    String HEADER_NORMALIZATION_ROOT_PUT = "${PREFIX},name=\".\\*_POST\""
    String HEADER_NORMALIZATION_RESOURCE_POST = "${PREFIX},name=\"/resource/(.\\*)_POST\""
    String HEADER_NORMALIZATION_RESOURCE_PUT = "${PREFIX},name=\"/resource/(.\\*)_POST\""
    String HEADER_NORMALIZATION_SERVERS_GET = "${PREFIX},name=\"/servers/(.\\*)_GET\""
    String HEADER_NORMALIZATION_SERVERS_POST = "${PREFIX},name=\"/servers/(.\\*)_POST\""
    String HEADER_NORMALIZATION_SERVERS_PUT = "${PREFIX},name=\"/servers/(.\\*)_POST\""
    String HEADER_NORMALIZATION_SECONDARY_PATH_GET = "${PREFIX},name=\"/secondary/path/(.\\*)_GET\""
    String HEADER_NORMALIZATION_TERTIARY_PATH_GET = "${PREFIX},name=\"/tertiary/path/(.\\*)_GET\""
    String HEADER_NORMALIZATION_ACROSS_ALL = "${PREFIX},name=\"ACROSS ALL\""


    def cleanup() {
        deproxy.shutdown()
        repose.stop()
    }

    def "when a client makes requests, jmx should keep accurate count"() {
        given:
        repose.applyConfigs(
                "features/filters/headerNormalization/metrics/single"
        )
        repose.start()

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        when:
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 2

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint, method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_PUT, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 3

        when:
        mc = deproxy.makeRequest(url: "${reposeEndpoint}/resource/1243/", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_RESOURCE_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 4

        when:
        mc = deproxy.makeRequest(url: "${reposeEndpoint}/resource/1243/", method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_RESOURCE_PUT, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 5

        when:
        mc = deproxy.makeRequest(url: "${reposeEndpoint}/servers/1243/", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 6

        when:
        mc = deproxy.makeRequest(url: "${reposeEndpoint}/servers/1243/", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 7

        when:
        mc = deproxy.makeRequest(url: "${reposeEndpoint}/servers/1243/", method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_PUT, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 8

    }

    def "when multiple filter instances are configured, each should add to the count"() {

        given:
        repose.applyConfigs(
                "features/filters/headerNormalization/metrics/multiple"
        )
        repose.start()

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        when: "client makes a request that matches one filter's uri-regex attribute"
        def mc = deproxy.makeRequest(url: reposeEndpoint)

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 1
        (repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") ?: 0) == 0
        (repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 1

        when: "client makes a request that matches two filters' uri-regex attributes"
        mc = deproxy.makeRequest(url: "${reposeEndpoint}/secondary/path/asdf")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        (repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 3


        when: "client makes a request that matches two filters' uri-regex attributes"
        mc = deproxy.makeRequest(url: "${reposeEndpoint}/tertiary/path/asdf")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 5

    }


}
