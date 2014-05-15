package features.filters.apivalidator

import framework.ReposeLogSearch
import framework.ReposeValveTest
import org.apache.commons.lang.RandomStringUtils
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 5/15/14.
 */
class CFApiValidatorContentLimitTest extends ReposeValveTest{
    String charset = (('a'..'z') + ('0'..'9')).join()
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/cfcontentlimit", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup () {
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.cleanLog()
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "should return 413 on request content larger than body limit"(){

        given: "I have a request body that exceed the header size limit"
        def text = RandomStringUtils.random(256, charset)
        def body = """<?xml version="1.0"?>
                      <entry xmlns="http://www.w3.org/2005/Atom">
                          <title>CF test</title>
                          <author><name>JV</name></author>
                          <content type="text">"""+ text +"""</content>
                          <category term="cloudfeed" />
                      </entry>"""
        def headers = ['Content-Type': 'application/atom+xml']
        println body.length()

        when: "I send a request to REPOSE with my request body"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/perftest1/events", headers: headers, requestBody: body, method: "POST")

        then: "Reponse with 413"
        mc.receivedResponse.code == "413"
        mc.receivedResponse.message == "Request Entity Too Large"
        mc.handlings.size() == 0
    }

    def "should return 200 on request within content body limit"(){

        given: "I have a request body that exceed the header size limit"
        def text = RandomStringUtils.random(10, charset)
        def body = """<?xml version="1.0"?>
                      <entry xmlns="http://www.w3.org/2005/Atom">
                          <title>CF test</title>
                          <author><name>JV</name></author>
                          <content type="text">"""+ text +"""</content>
                          <category term="cloudfeed" />
                      </entry>"""
        println body.length()

        def headers = ['Content-Type': 'application/atom+xml']

        when: "I send a request to REPOSE with my request body"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/perftest1/events", headers: headers, requestBody: body, method: "POST")

        then: "Reponse with 200"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }
}
