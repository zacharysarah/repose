package org.openrepose.filters.translation.httpx.node;

import org.openrepose.core.httpx.Response;
import org.openrepose.core.httpx.ResponseHead;
import org.openrepose.core.httpx.ResponseHeadDetail;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fran
 */
public class ResponseHeadNode extends ObjectFactoryUser implements ComplexNode {
    private final List<Node> nodes = new ArrayList<Node>();
    private final HttpServletResponse response;
    private final Response message;
    private final List<ResponseHeadDetail> headFidelity;
    private final List<String> headersFidelity;

    public ResponseHeadNode(HttpServletResponse response, Response message, List<ResponseHeadDetail> headFidelity, List<String> headersFidelity) {
        this.response = response;
        this.message = message;
        this.headFidelity = headFidelity;
        this.headersFidelity = headersFidelity;
    }

    @Override
    public void build() {                
        ResponseHead head = getObjectFactory().createResponseHead();

        head.getFidelity().addAll(headFidelity);

        for (ResponseHeadDetail headDetail : headFidelity) {

            switch (headDetail) {
                case HEADERS:
                    this.addChildNode(new ResponseHeadersNode(response, head, headersFidelity));
                    break;
            }
        }

        for (Node node : nodes) {
            node.build();
        }

        message.setHead(head);
    }

    @Override
    public void addChildNode(Node node) {
        nodes.add(node);
    }
}
