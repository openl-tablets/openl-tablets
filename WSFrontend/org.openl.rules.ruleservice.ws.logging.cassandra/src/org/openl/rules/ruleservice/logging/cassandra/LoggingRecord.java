package org.openl.rules.ruleservice.logging.cassandra;

import java.util.Date;

import org.openl.rules.ruleservice.logging.annotation.SetterIncomingTime;
import org.openl.rules.ruleservice.logging.annotation.SetterInputName;
import org.openl.rules.ruleservice.logging.annotation.SetterOutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.SetterPublisher;
import org.openl.rules.ruleservice.logging.annotation.SetterRequest;
import org.openl.rules.ruleservice.logging.annotation.SetterResponse;
import org.openl.rules.ruleservice.logging.annotation.SetterServiceName;
import org.openl.rules.ruleservice.logging.annotation.SetterUrl;
import org.openl.rules.ruleservice.logging.annotation.UseLoggingInfoConvertor;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(name = "openl_logging")
public class LoggingRecord {
    @PartitionKey(0)
    private String id;

    private Date incomingTime;
    private Date outcomingTime;
    private String request;
    private String response;
    @ClusteringColumn(1)
    private String serviceName;
    private String url;
    private String inputName;
    @ClusteringColumn(0)
    private String publisherType;

    public LoggingRecord() {
    }

    public LoggingRecord(String id,
            Date incomingTime,
            Date outcomingTime,
            String request,
            String response,
            String serviceName,
            String url,
            String inputName,
            String publisherType) {
        this.id = id;
        this.incomingTime = incomingTime;
        this.outcomingTime = outcomingTime;
        this.request = request;
        this.response = response;
        this.serviceName = serviceName;
        this.url = url;
        this.inputName = inputName;
        this.publisherType = publisherType;
    }

    public String getId() {
        return id;
    }

    @UseLoggingInfoConvertor(convertor = TimeBasedUUID.class)
    public void setId(String id) {
        this.id = id;
    }

    public Date getIncomingTime() {
        return incomingTime;
    }

    @SetterIncomingTime
    public void setIncomingTime(Date incomingTime) {
        this.incomingTime = incomingTime;
    }

    public Date getOutcomingTime() {
        return outcomingTime;
    }

    @SetterOutcomingTime
    public void setOutcomingTime(Date outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    public String getRequest() {
        return request;
    }

    @SetterRequest
    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    @SetterResponse
    public void setResponse(String response) {
        this.response = response;
    }

    public String getServiceName() {
        return serviceName;
    }

    @SetterServiceName
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUrl() {
        return url;
    }

    @SetterUrl
    public void setUrl(String url) {
        this.url = url;
    }

    public String getInputName() {
        return inputName;
    }

    @SetterInputName
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getPublisherType() {
        return publisherType;
    }

    @SetterPublisher
    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }

}
