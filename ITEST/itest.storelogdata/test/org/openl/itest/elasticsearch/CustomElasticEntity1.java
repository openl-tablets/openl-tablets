package org.openl.itest.elasticsearch;

import java.util.Date;

import org.openl.itest.cassandra.IntToStringConvertor;
import org.openl.itest.cassandra.NoConvertorString;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.storelogdata.annotation.IncomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.KafkaMessageHeader;
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
import org.openl.rules.ruleservice.storelogdata.annotation.ZonedDataTimeToDateConvertor;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.JSONRequest;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.JSONResponse;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.RandomUUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "openl_log_custom_index_1")
public class CustomElasticEntity1 {

    @WithStoreLogDataConverter(converter = RandomUUID.class)
    @Id
    private String id;

    @IncomingTime(converter = ZonedDataTimeToDateConvertor.class)
    private Date incomingTime;

    @OutcomingTime(converter = ZonedDataTimeToDateConvertor.class)
    private Date outcomingTime;

    @Request
    private String requestBody;

    @Response
    private String responseBody;

    @WithStoreLogDataConverter(converter = JSONRequest.class)
    @QualifyPublisherType(PublisherType.RESTFUL)
    private Object request;

    @WithStoreLogDataConverter(converter = JSONResponse.class)
    @QualifyPublisherType(PublisherType.RESTFUL)
    private Object response;

    @ServiceName
    private String serviceName;

    private String methodName;

    @Publisher
    private String publisherType;

    @Url
    private String url;

    private Integer hour;

    private String value;

    @Value(value = "result", converter = NoConvertorString.class)
    private String result;

    @Value(value = "intValue1")
    private Integer intValue1;

    @Value(value = "intValue2")
    private Integer intValue2;

    @Value(value = "intValue3")
    private Integer intValue3;

    @Value(value = "boolValue1")
    private boolean boolValue1;

    @Value(value = "boolValue2")
    private boolean boolValue2;

    @Value(value = "objectSerializerFound")
    private boolean objectSerializerFound;

    @Value(value = "stringValue1", converter = NoConvertorString.class)
    private String stringValue1;

    @Value(value = "stringValue2", converter = NoConvertorString.class)
    private String stringValue2;

    @Value(value = "stringValue3", converter = NoConvertorString.class)
    private String stringValue3;

    @KafkaMessageHeader(value = KafkaHeaders.METHOD_NAME)
    private String header1;

    @KafkaMessageHeader(value = "testHeader")
    private String header2;

    @Value(value = "intValueToString", converter = IntToStringConvertor.class)
    String intValueToString;

    public CustomElasticEntity1() {
    }

    @MethodName
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Publisher
    public String getPublisherType() {
        return publisherType;
    }

    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    @Value("hour")
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

    public String getMethodName() {
        return methodName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getIntValue1() {
        return intValue1;
    }

    public void setIntValue1(Integer intValue1) {
        this.intValue1 = intValue1;
    }

    public Integer getIntValue2() {
        return intValue2;
    }

    public void setIntValue2(Integer intValue2) {
        this.intValue2 = intValue2;
    }

    public Integer getIntValue3() {
        return intValue3;
    }

    public void setIntValue3(Integer intValue3) {
        this.intValue3 = intValue3;
    }

    public boolean isBoolValue1() {
        return boolValue1;
    }

    public void setBoolValue1(boolean boolValue1) {
        this.boolValue1 = boolValue1;
    }

    public boolean isBoolValue2() {
        return boolValue2;
    }

    public void setBoolValue2(boolean boolValue2) {
        this.boolValue2 = boolValue2;
    }

    public boolean isObjectSerializerFound() {
        return objectSerializerFound;
    }

    public void setObjectSerializerFound(boolean objectSerializerFound) {
        this.objectSerializerFound = objectSerializerFound;
    }

    public String getStringValue1() {
        return stringValue1;
    }

    public void setStringValue1(String stringValue1) {
        this.stringValue1 = stringValue1;
    }

    public String getStringValue2() {
        return stringValue2;
    }

    public void setStringValue2(String stringValue2) {
        this.stringValue2 = stringValue2;
    }

    public String getStringValue3() {
        return stringValue3;
    }

    public void setStringValue3(String stringValue3) {
        this.stringValue3 = stringValue3;
    }

    public String getHeader1() {
        return header1;
    }

    public void setHeader1(String header1) {
        this.header1 = header1;
    }

    public String getHeader2() {
        return header2;
    }

    public void setHeader2(String header2) {
        this.header2 = header2;
    }

    public String getIntValueToString() {
        return intValueToString;
    }

    public void setIntValueToString(String intValueToString) {
        this.intValueToString = intValueToString;
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
}
