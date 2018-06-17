package org.openl.rules.ruleservice.logging;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.cxf.interceptor.LoggingMessage;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

public class LoggingInfo {
    
    private RuleServiceLogging ruleServiceLogging;
    
    public LoggingInfo(RuleServiceLogging ruleServiceLoggingInfo) {
        this.ruleServiceLogging = ruleServiceLoggingInfo;
    }

    public PublisherType getPublisherType() {
        return ruleServiceLogging.getPublisherType();
    }

    public LoggingMessage getRequestMessage() {
        return ruleServiceLogging.getRequestMessage();
    }

    public LoggingMessage getResponseMessage() {
        return ruleServiceLogging.getResponseMessage();
    }

    public String getServiceName() {
        return ruleServiceLogging.getServiceName();
    }

    public Date getIncomingMessageTime() {
        return ruleServiceLogging.getIncomingMessageTime();
    }

    public Date getOutcomingMessageTime() {
        return ruleServiceLogging.getOutcomingMessageTime();
    }

    public String getInputName() {
        return ruleServiceLogging.getInputName();
    }

    public Object[] getParameters() {
        return ruleServiceLogging.getParameters();
    }

    public LoggingCustomData getLoggingCustomData() {
        return ruleServiceLogging.getLoggingCustomData();
    }

    public Method getServiceMethod() {
        return ruleServiceLogging.getServiceMethod();
    }

    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(ruleServiceLogging.getContext());
    }

    public boolean isIgnorable() {
        return ruleServiceLogging.isIgnorable();
    }
    
    
}
