package org.openl.rules.ruleservice.logging;

import java.util.Date;

import org.apache.cxf.interceptor.LoggingMessage;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.core.OpenLService;

/**
 * Bean for data for logging to external source feature.
 * 
 * @author Marat Kamalov
 *
 */
public class LoggingInfo {
    private LoggingMessage requestMessage;
    private LoggingMessage responseMessage;

    private Date incomingMessageTime;
    private Date outcomingMessageTime;

    private String inputName;
    private Object[] parameters;

    private OpenLService service;
    
    private PublisherType publisherType;
    
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

    public OpenLService getService() {
        return service;
    }

    public void setService(OpenLService service) {
        this.service = service;
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
}
