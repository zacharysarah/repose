package framework.mocks

import groovy.text.SimpleTemplateEngine
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

import java.util.concurrent.atomic.AtomicInteger

/**
 * Simulates responses from a Keystone v3 Service
 */
class MockKeystoneV3Service {

    private static final String X_AUTH_TOKEN_HEADER = "X-Auth-Token"
    private static final String X_SUBJECT_TOKEN_HEADER = "X-Subject-Token"
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    protected AtomicInteger _validateTokenCount = new AtomicInteger(0)
    protected AtomicInteger _generateTokenCount = new AtomicInteger(0)

    /*
     * The tokenExpiresAt field determines when the token expires. Consumers of
     * this class should set to a particular DateTime (for example, to test
     * some aspect of expiration dates), or leave it null to default to now
     * plus one day.
     */
    def tokenExpiresAt = null
    def client_token = 'this-is-the-token'
    def client_tenant = 'this-is-the-tenant'
    def client_username = 'username'
    def client_userid = 12345
    def admin_token = 'this-is-the-admin-token'
    def admin_tenant = 'this-is-the-admin-tenant'
    def admin_username = 'admin_username'
    def service_admin_role = 'service:admin-role1'
    def admin_userid = 67890
    def templateEngine = new SimpleTemplateEngine()
    def handler = { Request request -> return handleRequest(request) }

    boolean isTokenValid = true

    int port
    int originServicePort

    Closure<Response> validateTokenHandler
    Closure<Response> generateTokenHandler

    public MockKeystoneV3Service(int identityPort, int originServicePort) {
        resetHandlers()

        this.port = identityPort
        this.originServicePort = originServicePort
    }

    void resetCounts() {
        _validateTokenCount.set(0)
        _generateTokenCount.set(0)
    }

    public int getValidateTokenCount() {
        return _validateTokenCount.get()
    }

    public int getGenerateTokenCount() {
        return _generateTokenCount.get()
    }

    void resetHandlers() {
        handler = this.&handleRequest
        validateTokenHandler = this.&validateToken
        generateTokenHandler = this.&generateToken
    }

    // we can still use the `handler' closure even if handleRequest is overridden in a derived class
    Response handleRequest(Request request) {
        /*
         * From http://developer.openstack.org/api-ref-identity-v3.html
         *
         * POST
         * /v3/auth/tokens
         * Authenticates and generates a token.
         *
         * GET
         * /v3/auth/tokens
         * Validates a specified token.
         *
         */

        def path = request.path
        def method = request.method

        String nonQueryPath
        if (path.contains("?")) {
            int index = path.indexOf("?")
            nonQueryPath = path.substring(0, index)
        } else {
            nonQueryPath = path
        }

        if (nonQueryPath.equalsIgnoreCase("/v3/auth/tokens")) {
            if (method == "POST") {
                _generateTokenCount.incrementAndGet()
                return generateTokenHandler(request)
            } else if (method == "GET") {
                _validateTokenCount.incrementAndGet()
                def tokenId = request.getHeaders().getFirstValue(X_SUBJECT_TOKEN_HEADER)
                return validateTokenHandler(tokenId, request)
            } else {
                return new Response(405)
            }
        } else if (nonQueryPath.startsWith("/v3/users/")) {
            // TODO
        } else if (nonQueryPath.startsWith("/v3/domains/")) {
            // TODO
        }

        return new Response(501)
    }

    String getIssued() {
        return new DateTime()
    }

    String getExpires() {
        if (this.tokenExpiresAt != null && this.tokenExpiresAt instanceof String) {
            return this.tokenExpiresAt
        } else if (this.tokenExpiresAt instanceof DateTime) {
            DateTimeFormatter fmt = DateTimeFormat.forPattern(DATE_FORMAT).withLocale(Locale.US).withZone(DateTimeZone.UTC)
            return fmt.print(tokenExpiresAt)
        } else if (this.tokenExpiresAt) {
            return this.tokenExpiresAt.toString()
        } else {
            def now = new DateTime()
            def nowPlusOneDay = now.plusDays(1)
            return nowPlusOneDay
        }
    }

    Response validateToken(String tokenId, Request request, boolean xml) {
        def path = request.getPath()
        def request_token = tokenId

        def params = [
                expires     : getExpires(),
                issued      : getIssued(),
                userid      : client_userid,
                username    : client_username,
                tenant      : client_tenant,
                token       : request_token,
                serviceadmin: service_admin_role
        ]
        def code
        def template
        def headers = [:]
        if (xml) {
            headers.put('Content-type', 'application/xml')
        } else {
            headers.put('Content-type', 'application/json')
        }

        if (isTokenValid) {
            code = 200
            if (xml) {
                if (tokenId == "rackerButts") {
                    template = rackerTokenXmlTemplate
                } else if (tokenId == "failureRacker") {
                    template = rackerTokenWithoutProperRoleXmlTemplate
                } else {
                    template = identitySuccessXmlTemplate
                }
            } else {
                template = identitySuccessJsonTemplate
            }
        } else {
            code = 404
            if (xml) {
                template = identityFailureXmlTemplate
            } else {
                template = identityFailureJsonTemplate
            }
        }

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(code, null, headers, body)
    }

    Response generateToken(Request request) {
        try {
            // TODO: Validate what we need is present in the JSON request
        } catch (Exception e) {
            println("Admin token JSON validation error: " + e)
            return new Response(400)
        }

        def params = [
                expires     : getExpires(),
                issued      : getIssued(),
                userid      : admin_userid,
                username    : admin_username,
                tenant      : admin_tenant,
                token       : admin_token,
                serviceadmin: service_admin_role
        ]
        def code
        def template
        def headers = [:]
        headers.put('Content-type', 'application/json')

        if (isTokenValid) {
            code = 200
            template = identitySuccessJsonTemplate
        } else {
            code = 404
            template = identityFailureJsonTemplate
        }

        def body = templateEngine.createTemplate(template).make(params)

        return new Response(code, null, headers, body)
    }

    // TODO: Replace this with builder
    def identityFailureJsonTemplate =
"""{
   "itemNotFound" : {
      "message" : "Invalid Token, not found.",
      "code" : 404
   }
}
"""

    // TODO: Replace this with builder
    def identitySuccessJsonTemplate =
"""{
   "access" : {
      "serviceCatalog" : [
         {
            "name" : "cloudFilesCDN",
            "type" : "rax:object-cdn",
            "endpoints" : [
               {
                  "publicURL" : "https://cdn.stg.clouddrive.com/v1/\${tenant}",
                  "tenantId" : "\${tenant}",
                  "region" : "DFW"
               },
               {
                  "publicURL" : "https://cdn.stg.clouddrive.com/v1/\${tenant}",
                  "tenantId" : "\${tenant}",
                  "region" : "ORD"
               }
            ]
         },
         {
            "name" : "cloudFiles",
            "type" : "object-store",
            "endpoints" : [
               {
                  "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/\${tenant}",
                  "publicURL" : "https://storage.stg.swift.racklabs.com/v1/\${tenant}",
                  "tenantId" : "\${tenant}",
                  "region" : "ORD"
               },
               {
                  "internalURL" : "https://snet-storage.stg.swift.racklabs.com/v1/\${tenant}",
                  "publicURL" : "https://storage.stg.swift.racklabs.com/v1/\${tenant}",
                  "tenantId" : "\${tenant}",
                  "region" : "DFW"
               }
            ]
         }
      ],
      "user" : {
         "roles" : [
            {
               "tenantId" : "\${tenant}",
               "name" : "compute:default",
               "id" : "684",
               "description" : "A Role that allows a user access to keystone Service methods"
            },
            {
               "name" : "identity:admin",
               "id" : "1",
               "description" : "Admin Role."
            }
         ],
         "RAX-AUTH:defaultRegion" : "the-default-region",
         "name" : "\${username}",
         "id" : "\${userid}"
      },
      "token" : {
         "tenant" : {
            "name" : "\${tenant}",
            "id" : "\${tenant}"
         },
         "id" : "\${token}",
         "expires" : "\${expires}"
      }
   }
}
"""
/* TODO
"""
{
    "token": {
        "expires_at": "\${expires}",
        "issued_at": "\${issued}",
        "methods": [
            "password"
        ],
        "user": {
            "domain": {
                "id": "1789d1",
                "links": {
                    "self": "http://identity:35357/v3/domains/1789d1"
                },
                "name": "example.com"
            }
            "email": "joe@example.com",
            "id": "0ca8f6",
            "links": {
                "self": "http://identity:35357/v3/users/0ca8f6"
            },
            "name": "Joe"
        }
    }
}
"""
*/
}
