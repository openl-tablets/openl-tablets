package org.openl.rules.ruleservice.storelogdata;

import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;

public class CollectOperationResourceInfoInterceptor extends AbstractPhaseInterceptor<Message> {

    public CollectOperationResourceInfoInterceptor(String phase) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
    }

    public CollectOperationResourceInfoInterceptor() {
        this(Phase.USER_LOGICAL);
    }

    @Override
    public void handleMessage(Message message) {
        injectServiceMethod(message);
    }

    @Override
    public void handleFault(Message message) {
        injectServiceMethod(message);
    }

    private static void injectServiceMethod(Message message) {
        StoreLogData storeLogData = StoreLogDataHolder.get();
        var operationResourceInfo = message.getExchange().get(OperationResourceInfo.class);
        if (operationResourceInfo != null) {
            var serviceMethod = operationResourceInfo.getAnnotatedMethod();
            storeLogData.setServiceMethod(serviceMethod);
        } else {
            var bop = message.getExchange().get(BindingOperationInfo.class);
            var md = (MethodDispatcher) message.getExchange()
                    .get(Service.class)
                    .get(MethodDispatcher.class.getName());
            var method = md.getMethod(bop);
            storeLogData.setServiceMethod(method);
        }
    }
}
