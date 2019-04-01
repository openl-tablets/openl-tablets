package org.openl.rules.ruleservice.logging;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.LoggingMessage;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

/**
 * Bean for data for logging to external source feature.
 *
 * @author Marat Kamalov
 *
 */
public class RuleServiceLogging {
    private LoggingMessage requestMessage;
    private LoggingMessage responseMessage;

    private Date incomingMessageTime;
    private Date outcomingMessageTime;

    private String inputName;
    private Object[] parameters;

    private String serviceName;

    private PublisherType publisherType;

    private Method serviceMethod;

    private LoggingCustomData loggingCustomData;

    private ObjectSerializer objectSerializer;

    private Map<String, Object> context = new HashMap<>();

    private boolean ignorable = false;

    public PublisherType getPublisherType() {
        return publisherType;
    }

    public void setPublisherType(PublisherType publisherType) {
        this.publisherType = publisherType;
    }

    public LoggingMessage getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(LoggingMessage requestMessage) {
        this.requestMessage = requestMessage;
    }

    public LoggingMessage getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(LoggingMessage responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Date getIncomingMessageTime() {
        return incomingMessageTime;
    }

    public void setIncomingMessageTime(Date incomingMessageTime) {
        this.incomingMessageTime = incomingMessageTime;
    }

    public Date getOutcomingMessageTime() {
        return outcomingMessageTime;
    }

    public void setOutcomingMessageTime(Date outcomingMessageTime) {
        this.outcomingMessageTime = outcomingMessageTime;
    }

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public LoggingCustomData getLoggingCustomData() {
        return loggingCustomData;
    }

    public void setLoggingCustomData(LoggingCustomData loggingCustomData) {
        this.loggingCustomData = loggingCustomData;
    }

    public Method getServiceMethod() {
        return serviceMethod;
    }

    public void setServiceMethod(Method serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public boolean isIgnorable() {
        return ignorable;
    }

    public void setIgnorable(boolean ignorable) {
        this.ignorable = ignorable;
    }

    public void ignore() {
        this.ignorable = true;
    }

    public ObjectSerializer getObjectSerializer() {
        return objectSerializer;
    }

    public void setObjectSerializer(ObjectSerializer objectSerializer) {
        this.objectSerializer = objectSerializer;
    }
}
