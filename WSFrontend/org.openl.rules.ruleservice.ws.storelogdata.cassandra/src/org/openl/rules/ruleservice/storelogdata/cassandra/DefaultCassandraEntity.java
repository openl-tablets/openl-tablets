package org.openl.rules.ruleservice.storelogdata.cassandra;

import java.time.ZonedDateTime;

import org.openl.rules.ruleservice.storelogdata.annotation.*;
import org.openl.rules.ruleservice.storelogdata.cassandra.annotation.EntitySupport;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;

@EntitySupport(DefaultCassandraEntityOperations.class)
@Entity
@CqlName("openl_log_data")
public class DefaultCassandraEntity {
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

    @Override
    public String toString() {
        return "CassandraStoreLoggingEntity [id=" + id + "]";
    }

}
