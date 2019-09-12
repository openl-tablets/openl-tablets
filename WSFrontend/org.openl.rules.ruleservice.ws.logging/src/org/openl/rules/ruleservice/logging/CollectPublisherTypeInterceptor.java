package org.openl.rules.ruleservice.logging;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

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
        RuleServiceStoreLoggingData ruleServiceStoreLoggingData = RuleServiceStoreLoggingDataolder.get();
        ruleServiceStoreLoggingData.setPublisherType(publisherType);
    }
}