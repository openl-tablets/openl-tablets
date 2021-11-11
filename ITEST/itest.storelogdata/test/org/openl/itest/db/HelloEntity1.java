package org.openl.itest.db;

import java.time.ZonedDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;

import org.openl.itest.cassandra.IntToStringConvertor;
import org.openl.itest.cassandra.NoConvertorString;
import org.openl.rules.ruleservice.kafka.KafkaHeaders;
import org.openl.rules.ruleservice.storelogdata.annotation.IncomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.KafkaMessageHeader;
import org.openl.rules.ruleservice.storelogdata.annotation.MethodName;
import org.openl.rules.ruleservice.storelogdata.annotation.OutcomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.Publisher;
import org.openl.rules.ruleservice.storelogdata.annotation.Request;
import org.openl.rules.ruleservice.storelogdata.annotation.Response;
import org.openl.rules.ruleservice.storelogdata.annotation.ServiceName;
import org.openl.rules.ruleservice.storelogdata.annotation.Url;
import org.openl.rules.ruleservice.storelogdata.annotation.Value;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;

@Entity(name = "openl_logging_hello_entity1")
public class HelloEntity1 {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openl_logging_hello_entity1_generator")
    @SequenceGenerator(name = "openl_logging_hello_entity1_generator", sequenceName = "openl_logging_hello_entity1_generator", allocationSize = 50)
    private Long id;

    @IncomingTime
    private ZonedDateTime incomingTime;

    @OutcomingTime
    private ZonedDateTime outcomingTime;

    @Request(converter = NoConvertorString.class)
    @Lob
    private String request;

    @Response(converter = NoConvertorString.class)
    @Lob
    private String response;

    @ClusteringColumn(1)
    @ServiceName(converter = NoConvertorString.class)
    private String serviceName;

    @Url(converter = NoConvertorString.class)
    private String url;

    private String methodName;

    @ClusteringColumn(0)
    private String publisherType;

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

    @Value(value = "awareInstancesFound")
    private boolean awareInstancesFound;

    @Value(value = "dbConnectionFound")
    private boolean dbConnectionFound;

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
    private String intValueToString;

    public HelloEntity1() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isAwareInstancesFound() {
        return awareInstancesFound;
    }

    public void setAwareInstancesFound(boolean awareInstancesFound) {
        this.awareInstancesFound = awareInstancesFound;
    }

    public boolean isDbConnectionFound() {
        return dbConnectionFound;
    }

    public void setDbConnectionFound(boolean dbConnectionFound) {
        this.dbConnectionFound = dbConnectionFound;
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

    @Override
    public String toString() {
        return "HelloEntity1 [id=" + id + "]";
    }

}
