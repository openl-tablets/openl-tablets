package org.openl.rules.ruleservice.logging;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.openl.rules.ruleservice.core.OpenLService;

/**
 * CXF interceptor for collecting service data for logging to external source feature.
 *
 * @author Marat Kamalov
 *
 */
public class CollectOpenLServiceInterceptor extends AbstractPhaseInterceptor<Message> {

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
        StoreLoggingData storeLoggingData = StoreLoggingDataHolder.get();
        storeLoggingData.setServiceName(service.getName());
    }
}
