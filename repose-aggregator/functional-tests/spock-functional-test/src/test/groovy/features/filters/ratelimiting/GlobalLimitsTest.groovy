package features.filters.ratelimiting
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class GlobalLimitsTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/globalRateLimit", params)
        repose.start()
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Request should be limited by the global limit, regardless of #user or #group.")
    def "All requests should be limited by the global limit."() {

        given:
        def response
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        when: "we make multiple requests"
        response = deproxy.makeRequest(method: method, url: reposeEndpoint + url, headers: headers)

        then: "it should limit based off of the global rate limit"
        response.receivedResponse.code == responseCode

        where:
        url                     | user      | group     | method    | responseCode
        //Same url and group. Different user. GET method. Should hit on 4th request.
        "/test1"                | "user1"   | "group1"  | "GET"     | "200"
        "/test1"                | "user2"   | "group1"  | "GET"     | "200"
        "/test1"                | "user3"   | "group1"  | "GET"     | "200"
        "/test1"                | "user4"   | "group1"  | "GET"     | "503"
        //Same url and user. Different group. GET method. Should hit on 4th request.
        "/test2"                | "user1"   | "group1"  | "GET"     | "200"
        "/test2"                | "user1"   | "group2"  | "GET"     | "200"
        "/test2"                | "user1"   | "group3"  | "GET"     | "200"
        "/test2"                | "user1"   | "group4"  | "GET"     | "503"
        //Same url and group. Different user. POST method. Should hit on 4th request.
        "/test3"                | "user1"   | "group1"  | "POST"    | "200"
        "/test3"                | "user2"   | "group1"  | "POST"    | "200"
        "/test3"                | "user3"   | "group1"  | "POST"    | "200"
        "/test3"                | "user4"   | "group1"  | "POST"    | "503"
        //Same url and user. Different group. POST method. Should hit on 4th request.
        "/test4"                | "user1"   | "group1"  | "POST"    | "200"
        "/test4"                | "user1"   | "group2"  | "POST"    | "200"
        "/test4"                | "user1"   | "group3"  | "POST"    | "200"
        "/test4"                | "user1"   | "group4"  | "POST"    | "503"
        //Same url and group. Different user. PUT method. Should hit on 4th request.
        "/test5"                | "user1"   | "group1"  | "PUT"     | "200"
        "/test5"                | "user2"   | "group1"  | "PUT"     | "200"
        "/test5"                | "user3"   | "group1"  | "PUT"     | "200"
        "/test5"                | "user4"   | "group1"  | "PUT"     | "503"
        //Same url and user. Different group. PUT method. Should hit on 4th request.
        "/test6"                | "user1"   | "group1"  | "PUT"     | "200"
        "/test6"                | "user1"   | "group2"  | "PUT"     | "200"
        "/test6"                | "user1"   | "group3"  | "PUT"     | "200"
        "/test6"                | "user1"   | "group4"  | "PUT"     | "503"
        //Same url and group. Different user. DELETE method. Should hit on 4th request.
        "/test7"                | "user1"   | "group1"  | "DELETE"  | "200"
        "/test7"                | "user2"   | "group1"  | "DELETE"  | "200"
        "/test7"                | "user3"   | "group1"  | "DELETE"  | "200"
        "/test7"                | "user4"   | "group1"  | "DELETE"  | "503"
        //Same url and user. Different group. DELETE method. Should hit on 4th request.
        "/test8"                | "user1"   | "group1"  | "DELETE"  | "200"
        "/test8"                | "user1"   | "group2"  | "DELETE"  | "200"
        "/test8"                | "user1"   | "group3"  | "DELETE"  | "200"
        "/test8"                | "user1"   | "group4"  | "DELETE"  | "503"
        //Same url and group. Different user. PATCH method. Should hit on 4th request.
        "/test9"                | "user1"   | "group1"  | "PATCH"   | "200"
        "/test9"                | "user2"   | "group1"  | "PATCH"   | "200"
        "/test9"                | "user3"   | "group1"  | "PATCH"   | "200"
        "/test9"                | "user4"   | "group1"  | "PATCH"   | "503"
        //Same url and user. Different group. PATCH method. Should hit on 4th request.
        "/test10"               | "user1"   | "group1"  | "PATCH"   | "200"
        "/test10"               | "user1"   | "group2"  | "PATCH"   | "200"
        "/test10"               | "user1"   | "group3"  | "PATCH"   | "200"
        "/test10"               | "user1"   | "group4"  | "PATCH"   | "503"
        //Same url and group. Different user. HEAD method. Should hit on 4th request.
        "/test11"               | "user1"   | "group1"  | "HEAD"    | "200"
        "/test11"               | "user2"   | "group1"  | "HEAD"    | "200"
        "/test11"               | "user3"   | "group1"  | "HEAD"    | "200"
        "/test11"               | "user4"   | "group1"  | "HEAD"    | "503"
        //Same url and user. Different group. HEAD method. Should hit on 4th request.
        "/test12"               | "user1"   | "group1"  | "HEAD"    | "200"
        "/test12"               | "user1"   | "group2"  | "HEAD"    | "200"
        "/test12"               | "user1"   | "group3"  | "HEAD"    | "200"
        "/test12"               | "user1"   | "group4"  | "HEAD"    | "503"
        //Same url and group. Different user. OPTIONS method. Should hit on 4th request.
        "/test13"               | "user1"   | "group1"  | "OPTIONS" | "200"
        "/test13"               | "user2"   | "group1"  | "OPTIONS" | "200"
        "/test13"               | "user3"   | "group1"  | "OPTIONS" | "200"
        "/test13"               | "user4"   | "group1"  | "OPTIONS" | "503"
        //Same url and user. Different group. OPTIONS method. Should hit on 4th request.
        "/test14"               | "user1"   | "group1"  | "OPTIONS" | "200"
        "/test14"               | "user1"   | "group2"  | "OPTIONS" | "200"
        "/test14"               | "user1"   | "group3"  | "OPTIONS" | "200"
        "/test14"               | "user1"   | "group4"  | "OPTIONS" | "503"
        //Same url and group. Different user. CONNECT method. Should hit on 4th request.
        "/test15"               | "user1"   | "group1"  | "CONNECT" | "200"
        "/test15"               | "user2"   | "group1"  | "CONNECT" | "200"
        "/test15"               | "user3"   | "group1"  | "CONNECT" | "200"
        "/test15"               | "user4"   | "group1"  | "CONNECT" | "503"
        //Same url and user. Different group. CONNECT method. Should hit on 4th request.
        "/test16"               | "user1"   | "group1"  | "CONNECT" | "200"
        "/test16"               | "user1"   | "group2"  | "CONNECT" | "200"
        "/test16"               | "user1"   | "group3"  | "CONNECT" | "200"
        "/test16"               | "user1"   | "group4"  | "CONNECT" | "503"
        //Same url and group. Different user. TRACE method. Should hit on 4th request.
        "/test17"               | "user1"   | "group1"  | "TRACE"   | "200"
        "/test17"               | "user2"   | "group1"  | "TRACE"   | "200"
        "/test17"               | "user3"   | "group1"  | "TRACE"   | "200"
        "/test17"               | "user4"   | "group1"  | "TRACE"   | "503"
        //Same url and user. Different group. TRACE method. Should hit on 4th request.
        "/test18"               | "user1"   | "group1"  | "TRACE"   | "200"
        "/test18"               | "user1"   | "group2"  | "TRACE"   | "200"
        "/test18"               | "user1"   | "group3"  | "TRACE"   | "200"
        "/test18"               | "user1"   | "group4"  | "TRACE"   | "503"
        //Same group and user. Different URL. Should *not* hit on 4th request.
        "/test19"               | "user1"   | "group1"  | "GET"     | "200"
        "/test19"               | "user1"   | "group1"  | "GET"     | "200"
        "/test19"               | "user1"   | "group1"  | "GET"     | "200"
        "/test20"               | "user1"   | "group1"  | "GET"     | "200"
        //Same url, user, method. Different group. Same request parameters. Should hit on 4th request.
        "/test3?requestParam=1" | "user1"   | "group1"  | "GET"     | "200"
        "/test3?requestParam=1" | "user1"   | "group1"  | "GET"     | "200"
        "/test3?requestParam=1" | "user1"   | "group1"  | "GET"     | "200"
        "/test3?requestParam=1" | "user1"   | "group1"  | "GET"     | "503"
        //Same url, user, method. Different group. Same request parameters. Should *not* hit on 4th request.
        "/test3?requestParam=1" | "user1"   | "group1"  | "GET"     | "200"
        "/test3?requestParam=2" | "user1"   | "group1"  | "GET"     | "200"
        "/test3?requestParam=3" | "user1"   | "group1"  | "GET"     | "200"
        "/test3?requestParam=4" | "user1"   | "group1"  | "GET"     | "200"
    }
}
