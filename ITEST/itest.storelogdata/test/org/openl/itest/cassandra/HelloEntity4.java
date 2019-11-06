package org.openl.itest.cassandra;

import java.time.ZonedDateTime;

import org.openl.rules.ruleservice.storelogdata.annotation.*;
import org.openl.rules.ruleservice.storelogdata.cassandra.TimeBasedUUID;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.EntitySupport;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

@Entity
@EntitySupport(HelloEntity4Operations.class)
@CqlName("openl_logging_hello_entity4")
@QualifyPublisherType(PublisherType.WEBSERVICE)
public class HelloEntity4 {
    @PartitionKey(0)
    @WithStoreLogDataConverter(converter = TimeBasedUUID.class)
    private String id;

    @IncomingTime
    private ZonedDateTime incomingTime;

    @OutcomingTime
    private ZonedDateTime outcomingTime;

    @Request
    private String request;

    @Response
    private String response;

    @ClusteringColumn(1)
    @ServiceName
    private String serviceName;

    @Url
    private String url;

    @MethodName
    private String methodName;

    @ClusteringColumn(0)
    @Publisher
    private String publisherType;

    @Value("hour")
    private Integer hour;

    private String value;

    private String result;

    public HelloEntity4() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getIncomingTime() {
        return incomingTime;
    }

    public void setIncomingTime(ZonedDateTime incomingTime) {
        this.incomingTime = incomingTime;
    }

    public ZonedDateTime getOutcomingTime() {
        return outcomingTime;
    }

    public void setOutcomingTime(ZonedDateTime outcomingTime) {
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
    public void setValue(String value) {
        this.value = value;
    }

    @Value("result")
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "HelloEntity4 [id=" + id + "]";
    }

}
