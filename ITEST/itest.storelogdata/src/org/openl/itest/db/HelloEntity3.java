package org.openl.itest.db;

import static org.openl.itest.db.DBFields.HOUR;
import static org.openl.itest.db.DBFields.VALUE;

import java.time.ZonedDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;

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

@Entity(name = "openl_logging_hello_entity3")
public class HelloEntity3 {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openl_logging_hello_entity3_generator")
    @SequenceGenerator(name = "openl_logging_hello_entity3_generator", sequenceName = "openl_logging_hello_entity3_generator", allocationSize = 50)
    private Long id;

    @IncomingTime
    @QualifyPublisherType(PublisherType.KAFKA)
    private ZonedDateTime incomingTime;

    @OutcomingTime
    @QualifyPublisherType(PublisherType.WEBSERVICE)
    private ZonedDateTime outcomingTime;

    @Request
    @QualifyPublisherType(PublisherType.KAFKA)
    @Lob
    private String request;

    @Response
    @QualifyPublisherType(PublisherType.KAFKA)
    @Lob
    private String response;

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
    @Column(name = HOUR)
    @QualifyPublisherType(PublisherType.KAFKA)
    private Integer hour;

    @Column(name = VALUE)
    private String value;

    private String result;

    public HelloEntity3() {
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
        return "HelloEntity3 [id=" + id + "]";
    }

}
