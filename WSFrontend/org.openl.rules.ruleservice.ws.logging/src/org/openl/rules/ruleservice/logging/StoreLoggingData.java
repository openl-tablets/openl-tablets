package org.openl.rules.ruleservice.logging;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.LoggingMessage;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

public class StoreLoggingData {

    private RuleServiceStoreLoggingData ruleServiceStoreLoggingData;

    private Map<String, Object> loggingContext = new HashMap<>();

    public StoreLoggingData(RuleServiceStoreLoggingData ruleServiceStoreLoggingData) {
        this.ruleServiceStoreLoggingData = ruleServiceStoreLoggingData;
    }

    public PublisherType getPublisherType() {
        return ruleServiceStoreLoggingData.getPublisherType();
    }

    public LoggingMessage getRequestMessage() {
        return ruleServiceStoreLoggingData.getRequestMessage();
    }

    public LoggingMessage getResponseMessage() {
        return ruleServiceStoreLoggingData.getResponseMessage();
    }

    public String getServiceName() {
        return ruleServiceStoreLoggingData.getServiceName();
    }

    public Date getIncomingMessageTime() {
        return ruleServiceStoreLoggingData.getIncomingMessageTime();
    }

    public Date getOutcomingMessageTime() {
        return ruleServiceStoreLoggingData.getOutcomingMessageTime();
    }

    public String getInputName() {
        return ruleServiceStoreLoggingData.getInputName();
    }

    public Object[] getParameters() {
        return ruleServiceStoreLoggingData.getParameters();
    }

    public CustomData getCustomData() {
        return ruleServiceStoreLoggingData.getCustomData();
    }

    public Method getServiceMethod() {
        return ruleServiceStoreLoggingData.getServiceMethod();
    }

    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(ruleServiceStoreLoggingData.getContext());
    }

    public Map<String, Object> getLoggingContext() {
        return loggingContext;
    }

    public boolean isIgnorable() {
        return ruleServiceStoreLoggingData.isIgnorable();
    }

}
