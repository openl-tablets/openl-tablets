package org.openl.rules.ruleservice.logging;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.cxf.interceptor.LoggingMessage;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

public class LoggingInfo {
    
    private RuleServiceLoggingInfo ruleServiceLoggingInfo;
    
    public LoggingInfo(RuleServiceLoggingInfo ruleServiceLoggingInfo) {
        this.ruleServiceLoggingInfo = ruleServiceLoggingInfo;
    }

    public PublisherType getPublisherType() {
        return ruleServiceLoggingInfo.getPublisherType();
    }

    public LoggingMessage getRequestMessage() {
        return ruleServiceLoggingInfo.getRequestMessage();
    }

    public LoggingMessage getResponseMessage() {
        return ruleServiceLoggingInfo.getResponseMessage();
    }

    public String getServiceName() {
        return ruleServiceLoggingInfo.getServiceName();
    }

    public Date getIncomingMessageTime() {
        return ruleServiceLoggingInfo.getIncomingMessageTime();
    }

    public Date getOutcomingMessageTime() {
        return ruleServiceLoggingInfo.getOutcomingMessageTime();
    }

    public String getInputName() {
        return ruleServiceLoggingInfo.getInputName();
    }

    public Object[] getParameters() {
        return ruleServiceLoggingInfo.getParameters();
    }

    public LoggingCustomData getLoggingCustomData() {
        return ruleServiceLoggingInfo.getLoggingCustomData();
    }

    public Method getServiceMethod() {
        return ruleServiceLoggingInfo.getServiceMethod();
    }

    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(ruleServiceLoggingInfo.getContext());
    }

    public boolean isIgnorable() {
        return ruleServiceLoggingInfo.isIgnorable();
    }
    
    
}
