package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.Date;

import org.openl.rules.ruleservice.logging.annotation.IncomingTime;
import org.openl.rules.ruleservice.logging.annotation.InputName;
import org.openl.rules.ruleservice.logging.annotation.OutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.Publisher;
import org.openl.rules.ruleservice.logging.annotation.PublisherType;
import org.openl.rules.ruleservice.logging.annotation.QualifyPublisherType;
import org.openl.rules.ruleservice.logging.annotation.Request;
import org.openl.rules.ruleservice.logging.annotation.Response;
import org.openl.rules.ruleservice.logging.annotation.ServiceName;
import org.openl.rules.ruleservice.logging.annotation.Url;
import org.openl.rules.ruleservice.logging.annotation.WithStoreLoggingDataConvertor;
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

    @WithStoreLoggingDataConvertor(convertor = RandomUUID.class)
    public void setId(String id) {
        this.id = id;
    }

    @IncomingTime
    public void setIncomingTime(Date incomingTime) {
        this.incomingTime = incomingTime;
    }

    @OutcomingTime
    public void setOutcomingTime(Date outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    @Request
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    @Response
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    @WithStoreLoggingDataConvertor(convertor = JSONRequest.class)
    @QualifyPublisherType(PublisherType.RESTFUL)
    public void setRequest(Object request) {
        this.request = request;
    }

    @WithStoreLoggingDataConvertor(convertor = JSONResponse.class)
    @QualifyPublisherType(PublisherType.RESTFUL)
    public void setResponse(Object response) {
        this.response = response;
    }

    @ServiceName
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @InputName
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    @Publisher
    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    @Url
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }
}
