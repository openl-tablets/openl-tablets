package org.openl.rules.ruleservice.storelogdata;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CXF interceptor for collecting service data for logging to external source feature.
 *
 * @author Marat Kamalov
 *
 */
public class CollectOpenLServiceInterceptor extends AbstractPhaseInterceptor<Message> {
    private final Logger log = LoggerFactory.getLogger(CollectOpenLServiceInterceptor.class);

    private OpenLService service;

    public CollectOpenLServiceInterceptor(String phase, OpenLService service) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
        this.service = service;
    }

    public CollectOpenLServiceInterceptor(OpenLService service) {
        this(Phase.PRE_STREAM, service);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        injectServiceName(message);
    }

    @Override
    public void handleFault(Message message) {
        injectServiceName(message);
    }

    private void injectServiceName(Message message) {
        StoreLogData storeLogData = StoreLogDataHolder.get();
        storeLogData.setServiceName(service.getName());
        try {
            storeLogData.setServiceClass(service.getServiceClass());
        } catch (RuleServiceInstantiationException e) {
            log.error("Unexpected exception.", e);
        }
    }
}
