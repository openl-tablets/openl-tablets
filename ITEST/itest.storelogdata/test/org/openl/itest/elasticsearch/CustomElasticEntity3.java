package org.openl.itest.elasticsearch;

import java.time.ZonedDateTime;

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
import org.openl.rules.ruleservice.storelogdata.annotation.WithStoreLogDataConverter;
import org.openl.rules.ruleservice.storelogdata.cassandra.TimeBasedUUID;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.JSONRequest;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.JSONResponse;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "openl_log_custom_index_3")
public class CustomElasticEntity3 {

    @Id
    @WithStoreLogDataConverter(converter = TimeBasedUUID.class)
    private String id;

    @IncomingTime
    @QualifyPublisherType(PublisherType.KAFKA)
    private ZonedDateTime incomingTime;

    @OutcomingTime
    @QualifyPublisherType(PublisherType.WEBSERVICE)
    private ZonedDateTime outcomingTime;

    @Request
    @QualifyPublisherType(PublisherType.KAFKA)
    private String requestBody;

    @Response
    @QualifyPublisherType(PublisherType.KAFKA)
    private String responseBody;

    @WithStoreLogDataConverter(converter = JSONRequest.class)
    @QualifyPublisherType(PublisherType.KAFKA)
    private Object request;

    @WithStoreLogDataConverter(converter = JSONResponse.class)
    @QualifyPublisherType(PublisherType.KAFKA)
    private Object response;

    @ServiceName
    private String serviceName;

    @Url
    @QualifyPublisherType(PublisherType.WEBSERVICE)
    private String url;

    @MethodName
    @QualifyPublisherType(PublisherType.KAFKA)
    private String methodName;

    @Publisher
    private String publisherType;

    @Value("hour")
    @QualifyPublisherType(PublisherType.KAFKA)
    private Integer hour;

    private String value;

    private String result;

    public CustomElasticEntity3() {
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

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
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

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "CustomElasticEntity3 [id=" + id + "]";
    }

}
