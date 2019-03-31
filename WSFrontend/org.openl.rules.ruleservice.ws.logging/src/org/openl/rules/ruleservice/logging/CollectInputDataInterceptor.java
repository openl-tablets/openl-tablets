package org.openl.rules.ruleservice.logging;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.OperationInfo;

/**
 * CXF interceptor for collecting service input parameters for logging to external source feature.
 * 
 * @author Marat Kamalov
 *
 */
public class CollectInputDataInterceptor extends AbstractPhaseInterceptor<Message> {
    public CollectInputDataInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    @Override
    public void handleMessage(Message message) {
        OperationInfo operationInfo = message.getExchange().get(OperationInfo.class);
        if (operationInfo != null) {
            String inputName = operationInfo.getInputName();
            RuleServiceLogging ruleServiceLogging = RuleServiceLoggingHolder.get();
            ruleServiceLogging.setInputName(inputName);
            MessageContentsList objs = MessageContentsList.getContentsList(message);
            if (objs != null) {
                Object[] params = new Object[objs.size()];
                for (int i = 0; i < params.length; i++) {
                    params[i] = objs.get(i);
                }
                ruleServiceLogging.setParameters(params);
            }
        }
    }
}