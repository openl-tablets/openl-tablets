package org.openl.rules.ruleservice.publish.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class JAXRS200StatusOutInterceptor extends AbstractPhaseInterceptor<Message> {

    boolean enabled = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public JAXRS200StatusOutInterceptor() {
        super(Phase.MARSHAL);
    }

    @Override
    public void handleMessage(Message message) {
        if (!enabled) {
            return;
        }
        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs == null || objs.isEmpty()) {
            return;
        }

        Object responseObj = objs.get(0);

        Response response = null;
        if (responseObj instanceof Response) {
            response = (Response) responseObj;
            if (response.getStatus() != Status.OK.getStatusCode() && response instanceof ResponseImpl) {
                ResponseImpl responseImpl = (ResponseImpl) response;
                responseImpl.setStatus(Status.OK.getStatusCode());
            }
        }
    }
}
