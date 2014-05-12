package features.filters.apivalidator

import framework.ReposeValveTest
import org.apache.commons.lang.RandomStringUtils
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 5/9/14.
 */
class ApiValidatorContentLimitTest extends ReposeValveTest{
    String charset = (('A'..'Z') + ('0'..'9')).join()
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/contentlimit", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("When sending a #reqMethod through repose")
    def "should return 413 on request content larger than body limit"(){

        given: "I have a request body that exceed the header size limit"
        def body = RandomStringUtils.random(1025, charset)

        when: "I send a request to REPOSE with my request body"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, requestBody: body, method: reqMethod)

        then: "Reponse with 413"
        mc.receivedResponse.code == "413"
        mc.receivedResponse.message == "Request Entity Too Large"
        mc.handlings.size() == 0


        where:
        reqMethod << ["POST","PUT","DELETE","PATCH"]
    }

    @Unroll("When sending a #reqMethod through repose request content within body limit")
    def "should return 200 on request content within body limit"(){

        given: "I have a request body that exceed the header size limit"
        def body = RandomStringUtils.random(1022, charset)

        when: "I send a request to REPOSE with my request body"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, requestBody: body, method: reqMethod)

        then: "Reponse with 200"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        where:
        reqMethod << ["POST","PUT","DELETE","PATCH"]
    }
}
