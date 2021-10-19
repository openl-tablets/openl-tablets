package org.openl.rules.ruleservice.storelogdata.db;

import java.time.ZonedDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.openl.rules.ruleservice.storelogdata.annotation.IncomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.MethodName;
import org.openl.rules.ruleservice.storelogdata.annotation.OutcomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.Publisher;
import org.openl.rules.ruleservice.storelogdata.annotation.Request;
import org.openl.rules.ruleservice.storelogdata.annotation.Response;
import org.openl.rules.ruleservice.storelogdata.annotation.ServiceName;
import org.openl.rules.ruleservice.storelogdata.annotation.Url;

@Entity(name = "openl_log_data")
public class DefaultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openl_log_data_generator")
    @SequenceGenerator(name = "openl_log_data_generator", sequenceName = "openl_log_data_generator", allocationSize = 50)
    private Long id;

    @IncomingTime
    private ZonedDateTime incomingTime;

    @OutcomingTime
    private ZonedDateTime outcomingTime;

    @Request
    private String request;

    @Response
    private String response;

    @ServiceName
    private String serviceName;

    @Url
    private String url;

    @MethodName
    private String methodName;

    @Publisher
    private String publisherType;

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

    @Override
    public String toString() {
        return "DefaultEntity [id=" + id + "]";
    }
}
