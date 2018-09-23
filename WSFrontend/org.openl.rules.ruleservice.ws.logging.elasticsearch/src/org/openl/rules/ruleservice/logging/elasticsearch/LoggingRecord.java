package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.Date;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.logging.annotation.SetterIncomingTime;
import org.openl.rules.ruleservice.logging.annotation.SetterInputName;
import org.openl.rules.ruleservice.logging.annotation.SetterOutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.SetterPublisher;
import org.openl.rules.ruleservice.logging.annotation.SetterRequest;
import org.openl.rules.ruleservice.logging.annotation.SetterResponse;
import org.openl.rules.ruleservice.logging.annotation.SetterServiceName;
import org.openl.rules.ruleservice.logging.annotation.SetterUrl;
import org.openl.rules.ruleservice.logging.annotation.UseLoggingInfoConvertor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "loggingrecord")
public class LoggingRecord {
    @Id
    private String id;

    private Date incomingTime;
    private Date outcomingTime;

    private String requestBody;
    private String responseBody;

    private Object request;
    private Object response;
    
    private String serviceName;
    private String inputName;
    private String publisherType;

    private String url;
    
    public LoggingRecord() {
    }
    
    public LoggingRecord(String id,
            Date incomingTime,
            Date outcomingTime,
            Object request,
            Object response,
            String requestBody,
            String responseBody,
            String serviceName,
            String url,
            String inputName,
            String publisherType) {
        super();
        this.id = id;
        this.incomingTime = incomingTime;
        this.outcomingTime = outcomingTime;
        this.request = request;
        this.response = response;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.serviceName = serviceName;
        this.url = url;
        this.inputName = inputName;
        this.publisherType = publisherType;
    }

    public String getPublisherType() {
        return publisherType;
    }

    public String getId() {
        return id;
    }

    public Object getRequest() {
        return request;
    }

    public Object getResponse() {
        return response;
    }

    public Date getIncomingTime() {
        return incomingTime;
    }

    public Date getOutcomingTime() {
        return outcomingTime;
    }

    public String getInputName() {
        return inputName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUrl() {
        return url;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    @UseLoggingInfoConvertor(convertor = RandomUUID.class)
    public void setId(String id) {
        this.id = id;
    }

    @SetterIncomingTime
    public void setIncomingTime(Date incomingTime) {
        this.incomingTime = incomingTime;
    }

    @SetterOutcomingTime
    public void setOutcomingTime(Date outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    @SetterRequest
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    @SetterResponse
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    @UseLoggingInfoConvertor(convertor = JSONRequest.class, publisherTypes = PublisherType.RESTFUL)
    public void setRequest(Object request) {
        this.request = request;
    }

    @UseLoggingInfoConvertor(convertor = JSONResponse.class, publisherTypes = PublisherType.RESTFUL)
    public void setResponse(Object response) {
        this.response = response;
    }

    @SetterServiceName
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @SetterInputName
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    @SetterPublisher
    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    @SetterUrl
    public void setUrl(String url) {
        this.url = url;
    }
    
    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }
}
