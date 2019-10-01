package org.openl.rules.ruleservice.storelogdata;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataHolder;

public class CollectPublisherTypeInterceptor extends AbstractPhaseInterceptor<Message> {

    private PublisherType publisherType;

    public CollectPublisherTypeInterceptor(String phase, PublisherType publisherType) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
        this.publisherType = publisherType;
    }

    public CollectPublisherTypeInterceptor(PublisherType publisherType) {
        this(Phase.PRE_STREAM, publisherType);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        injectPublisherType(message);
    }

    @Override
    public void handleFault(Message message) {
        injectPublisherType(message);
    }

    private void injectPublisherType(Message message) {
        StoreLogData storeLogData = StoreLogDataHolder.get();
        storeLogData.setPublisherType(publisherType);
    }
}