package org.openl.rules.ruleservice.logging.cassandra;

import java.util.Date;

import org.openl.rules.ruleservice.logging.annotation.SetterCustomDateValue1;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomDateValue2;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomDateValue3;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue1;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue2;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue3;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue4;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomNumberValue5;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue1;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue2;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue3;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue4;
import org.openl.rules.ruleservice.logging.annotation.SetterCustomStringValue5;
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

    public String getStringValue1() {
        return stringValue1;
    }

    @SetterCustomStringValue1
    public void setStringValue1(String stringValue1) {
        this.stringValue1 = stringValue1;
    }

    public String getStringValue2() {
        return stringValue2;
    }

    @SetterCustomStringValue2
    public void setStringValue2(String stringValue2) {
        this.stringValue2 = stringValue2;
    }

    public String getStringValue3() {
        return stringValue3;
    }

    @SetterCustomStringValue3
    public void setStringValue3(String stringValue3) {
        this.stringValue3 = stringValue3;
    }

    public String getStringValue4() {
        return stringValue4;
    }

    @SetterCustomStringValue4
    public void setStringValue4(String stringValue4) {
        this.stringValue4 = stringValue4;
    }

    public String getStringValue5() {
        return stringValue5;
    }

    @SetterCustomStringValue5
    public void setStringValue5(String stringValue5) {
        this.stringValue5 = stringValue5;
    }

    public Long getNumberValue1() {
        return numberValue1;
    }

    @SetterCustomNumberValue1
    public void setNumberValue1(Long numberValue1) {
        this.numberValue1 = numberValue1;
    }

    public Long getNumberValue2() {
        return numberValue2;
    }

    @SetterCustomNumberValue2
    public void setNumberValue2(Long numberValue2) {
        this.numberValue2 = numberValue2;
    }

    public Long getNumberValue3() {
        return numberValue3;
    }

    @SetterCustomNumberValue3
    public void setNumberValue3(Long numberValue3) {
        this.numberValue3 = numberValue3;
    }

    public Long getNumberValue4() {
        return numberValue4;
    }

    @SetterCustomNumberValue4
    public void setNumberValue4(Long numberValue4) {
        this.numberValue4 = numberValue4;
    }

    public Long getNumberValue5() {
        return numberValue5;
    }

    @SetterCustomNumberValue5
    public void setNumberValue5(Long numberValue5) {
        this.numberValue5 = numberValue5;
    }

    public Date getDateValue1() {
        return dateValue1;
    }

    @SetterCustomDateValue1
    public void setDateValue1(Date dateValue1) {
        this.dateValue1 = dateValue1;
    }

    public Date getDateValue2() {
        return dateValue2;
    }

    @SetterCustomDateValue2
    public void setDateValue2(Date dateValue2) {
        this.dateValue2 = dateValue2;
    }

    public Date getDateValue3() {
        return dateValue3;
    }

    @SetterCustomDateValue3
    public void setDateValue3(Date dateValue3) {
        this.dateValue3 = dateValue3;
    }

    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }

}
