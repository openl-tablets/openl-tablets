package org.openl.rules.ruleservice.logging.cassandra;

import java.util.Date;

import org.openl.rules.ruleservice.logging.annotation.CustomDateValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomDateValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomDateValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue4;
import org.openl.rules.ruleservice.logging.annotation.CustomNumberValue5;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue1;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue2;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue3;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue4;
import org.openl.rules.ruleservice.logging.annotation.CustomStringValue5;
import org.openl.rules.ruleservice.logging.annotation.IncomingTime;
import org.openl.rules.ruleservice.logging.annotation.InputName;
import org.openl.rules.ruleservice.logging.annotation.OutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.Publisher;
import org.openl.rules.ruleservice.logging.annotation.Request;
import org.openl.rules.ruleservice.logging.annotation.Response;
import org.openl.rules.ruleservice.logging.annotation.ServiceName;
import org.openl.rules.ruleservice.logging.annotation.Url;
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
    private String stringValue1;
    private String stringValue2;
    private String stringValue3;
    private String stringValue4;
    private String stringValue5;
    private Long numberValue1;
    private Long numberValue2;
    private Long numberValue3;
    private Long numberValue4;
    private Long numberValue5;
    private Date dateValue1;
    private Date dateValue2;
    private Date dateValue3;

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
            String publisherType,
            String stringValue1,
            String stringValue2,
            String stringValue3,
            String stringValue4,
            String stringValue5,
            Long numberValue1,
            Long numberValue2,
            Long numberValue3,
            Long numberValue4,
            Long numberValue5,
            Date dateValue1,
            Date dateValue2,
            Date dateValue3) {
        this.id = id;
        this.incomingTime = incomingTime;
        this.outcomingTime = outcomingTime;
        this.request = request;
        this.response = response;
        this.serviceName = serviceName;
        this.url = url;
        this.inputName = inputName;
        this.publisherType = publisherType;
        this.stringValue1 = stringValue1;
        this.stringValue2 = stringValue2;
        this.stringValue3 = stringValue3;
        this.stringValue4 = stringValue4;
        this.stringValue5 = stringValue5;
        this.numberValue1 = numberValue1;
        this.numberValue2 = numberValue2;
        this.numberValue3 = numberValue3;
        this.numberValue4 = numberValue4;
        this.numberValue5 = numberValue5;
        this.dateValue1 = dateValue1;
        this.dateValue2 = dateValue2;
        this.dateValue3 = dateValue3;
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

    @IncomingTime
    public void setIncomingTime(Date incomingTime) {
        this.incomingTime = incomingTime;
    }

    public Date getOutcomingTime() {
        return outcomingTime;
    }

    @OutcomingTime
    public void setOutcomingTime(Date outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    public String getRequest() {
        return request;
    }

    @Request
    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    @Response
    public void setResponse(String response) {
        this.response = response;
    }

    public String getServiceName() {
        return serviceName;
    }

    @ServiceName
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUrl() {
        return url;
    }

    @Url
    public void setUrl(String url) {
        this.url = url;
    }

    public String getInputName() {
        return inputName;
    }

    @InputName
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getPublisherType() {
        return publisherType;
    }

    @Publisher
    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    public String getStringValue1() {
        return stringValue1;
    }

    @CustomStringValue1
    public void setStringValue1(String stringValue1) {
        this.stringValue1 = stringValue1;
    }

    public String getStringValue2() {
        return stringValue2;
    }

    @CustomStringValue2
    public void setStringValue2(String stringValue2) {
        this.stringValue2 = stringValue2;
    }

    public String getStringValue3() {
        return stringValue3;
    }

    @CustomStringValue3
    public void setStringValue3(String stringValue3) {
        this.stringValue3 = stringValue3;
    }

    public String getStringValue4() {
        return stringValue4;
    }

    @CustomStringValue4
    public void setStringValue4(String stringValue4) {
        this.stringValue4 = stringValue4;
    }

    public String getStringValue5() {
        return stringValue5;
    }

    @CustomStringValue5
    public void setStringValue5(String stringValue5) {
        this.stringValue5 = stringValue5;
    }

    public Long getNumberValue1() {
        return numberValue1;
    }

    @CustomNumberValue1
    public void setNumberValue1(Long numberValue1) {
        this.numberValue1 = numberValue1;
    }

    public Long getNumberValue2() {
        return numberValue2;
    }

    @CustomNumberValue2
    public void setNumberValue2(Long numberValue2) {
        this.numberValue2 = numberValue2;
    }

    public Long getNumberValue3() {
        return numberValue3;
    }

    @CustomNumberValue3
    public void setNumberValue3(Long numberValue3) {
        this.numberValue3 = numberValue3;
    }

    public Long getNumberValue4() {
        return numberValue4;
    }

    @CustomNumberValue4
    public void setNumberValue4(Long numberValue4) {
        this.numberValue4 = numberValue4;
    }

    public Long getNumberValue5() {
        return numberValue5;
    }

    @CustomNumberValue5
    public void setNumberValue5(Long numberValue5) {
        this.numberValue5 = numberValue5;
    }

    public Date getDateValue1() {
        return dateValue1;
    }

    @CustomDateValue1
    public void setDateValue1(Date dateValue1) {
        this.dateValue1 = dateValue1;
    }

    public Date getDateValue2() {
        return dateValue2;
    }

    @CustomDateValue2
    public void setDateValue2(Date dateValue2) {
        this.dateValue2 = dateValue2;
    }

    public Date getDateValue3() {
        return dateValue3;
    }

    @CustomDateValue3
    public void setDateValue3(Date dateValue3) {
        this.dateValue3 = dateValue3;
    }

    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }

}
