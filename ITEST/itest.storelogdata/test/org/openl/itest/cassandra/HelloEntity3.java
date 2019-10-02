package org.openl.itest.cassandra;

import java.util.Date;

import org.openl.rules.ruleservice.storelogdata.annotation.IncomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.MethodName;
import org.openl.rules.ruleservice.storelogdata.annotation.OutcomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.Publisher;
import org.openl.rules.ruleservice.storelogdata.annotation.PublisherType;
import org.openl.rules.ruleservice.storelogdata.annotation.QualifyPublisherType;
import org.openl.rules.ruleservice.storelogdata.annotation.Request;
import org.openl.rules.ruleservice.storelogdata.annotation.Response;
import org.openl.rules.ruleservice.storelogdata.annotation.ServiceName;
import org.openl.rules.ruleservice.storelogdata.annotation.Url;
import org.openl.rules.ruleservice.storelogdata.annotation.Value;
import org.openl.rules.ruleservice.storelogdata.annotation.WithStoreLogDataConvertor;
import org.openl.rules.ruleservice.storelogdata.cassandra.TimeBasedUUID;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(name = "openl_logging_hello_entity3")
public class HelloEntity3 {
    @PartitionKey(0)
    @WithStoreLogDataConvertor(convertor = TimeBasedUUID.class)
    private String id;

    @IncomingTime
    @QualifyPublisherType(PublisherType.KAFKA)
    private Date incomingTime;

    @OutcomingTime
    @QualifyPublisherType(PublisherType.WEBSERVICE)
    private Date outcomingTime;

    @Request
    @QualifyPublisherType(PublisherType.KAFKA)
    private String request;

    @Response
    @QualifyPublisherType(PublisherType.KAFKA)
    private String response;

    @ClusteringColumn(1)
    @ServiceName
    private String serviceName;

    @Url
    @QualifyPublisherType(PublisherType.WEBSERVICE)
    private String url;

    @MethodName
    @QualifyPublisherType(PublisherType.KAFKA)
    private String methodName;

    @ClusteringColumn(0)
    @Publisher
    private String publisherType;

    @Value("hour")
    @QualifyPublisherType(PublisherType.KAFKA)
    private Integer hour;

    private String value;

    private String result;

    public HelloEntity3() {
    }

    public HelloEntity3(String id,
            Date incomingTime,
            Date outcomingTime,
            String request,
            String response,
            String serviceName,
            String url,
            String methodName,
            String publisherType) {
        this.id = id;
        this.incomingTime = incomingTime;
        this.outcomingTime = outcomingTime;
        this.request = request;
        this.response = response;
        this.serviceName = serviceName;
        this.url = url;
        this.methodName = methodName;
        this.publisherType = publisherType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getIncomingTime() {
        return incomingTime;
    }

    public void setIncomingTime(Date incomingTime) {
        this.incomingTime = incomingTime;
    }

    public Date getOutcomingTime() {
        return outcomingTime;
    }

    public void setOutcomingTime(Date outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getPublisherType() {
        return publisherType;
    }

    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }

    public Integer getHour() {
        return hour;
    }

    public String getValue() {
        return value;
    }

    @Value("value1")
    @QualifyPublisherType(PublisherType.WEBSERVICE)
    public void setValue(String value) {
        this.value = value;
    }

    @Value("result")
    @QualifyPublisherType(PublisherType.KAFKA)
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "HelloEntity2 [id=" + id + "]";
    }

}
