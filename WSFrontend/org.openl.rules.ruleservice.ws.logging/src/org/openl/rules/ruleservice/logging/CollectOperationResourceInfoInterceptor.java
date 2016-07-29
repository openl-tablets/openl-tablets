package org.openl.rules.ruleservice.logging;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class CollectOperationResourceInfoInterceptor extends AbstractPhaseInterceptor<Message> {

    public CollectOperationResourceInfoInterceptor(String phase) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
    }

    public CollectOperationResourceInfoInterceptor() {
        this(Phase.USER_LOGICAL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        RuleServiceLoggingInfo ruleServiceLoggingInfo = RuleServiceLoggingInfoHolder.get();
        OperationResourceInfo operationResourceInfo = message.getExchange().get(OperationResourceInfo.class);
        ruleServiceLoggingInfo.setOperationResourceInfo(operationResourceInfo);
    }
}