package org.openl.rules.ruleservice.logging;

import java.lang.reflect.Method;

import org.apache.cxf.interceptor.Fault;
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
    public void handleMessage(Message message) throws Fault {
        injectServiceMethod(message);
    }

    @Override
    public void handleFault(Message message) {
        injectServiceMethod(message);
    }

    private void injectServiceMethod(Message message) {
        StoreLogData storeLogData = StoreLogDataHolder.get();
        OperationResourceInfo operationResourceInfo = message.getExchange().get(OperationResourceInfo.class);
        if (operationResourceInfo != null) {
            Method serviceMethod = operationResourceInfo.getAnnotatedMethod();
            storeLogData.setServiceMethod(serviceMethod);
        } else {
            BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
            MethodDispatcher md = (MethodDispatcher) message.getExchange()
                .get(Service.class)
                .get(MethodDispatcher.class.getName());
            Method method = md.getMethod(bop);
            storeLogData.setServiceMethod(method);
        }
    }
}