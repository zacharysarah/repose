package org.openrepose.filters.translation.httpx.node;

import org.openrepose.core.httpx.Body;
import org.openrepose.core.httpx.Response;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletResponse;

/**
 * @author fran
 */
public class ResponseBodyNode extends ObjectFactoryUser implements Node {
    private final HttpServletResponse response;
    private final Response messageResponse;
    private final boolean jsonProcessing;

    public ResponseBodyNode(HttpServletResponse response, Response messageResponse, boolean jsonProcessing) {
        this.response = response;
        this.messageResponse = messageResponse;
        this.jsonProcessing = jsonProcessing;
    }

    @Override
    public void build() {
        Body body = getObjectFactory().createBody();

        // TODO: Need to determine how we want to handle the response data and if it needs any processing
        
        messageResponse.setBody(body);
    }
    
    public HttpServletResponse getResponse() {
        return response;
    }
    
    public boolean getJsonProcessing() {
        return jsonProcessing;
    }
        
}
