package org.openl.rules.ruleservice.publish.jaxrs.wadl;

import javax.ws.rs.core.Response;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class DisableWADLInterceptor extends AbstractPhaseInterceptor<Message> {

    public DisableWADLInterceptor() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(Message message) {
        if ("_wadl".equals(message.getContextualProperty("org.apache.cxf.message.Message.QUERY_STRING"))) {
            Response response = Response.status(Response.Status.NOT_FOUND).build();
            message.getExchange().put(Response.class, response);
        }
    }

    @Override
    public void handleFault(Message message) {
    }

}
