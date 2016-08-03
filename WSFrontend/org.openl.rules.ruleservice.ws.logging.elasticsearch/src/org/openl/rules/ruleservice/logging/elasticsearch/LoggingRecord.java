package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.Date;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
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
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "loggingrecord")
public class LoggingRecord {
    @Id
    private String id;

    private Date incomingTime;
    private Date outcomingTime;

    private String requestBody;
    private String responseBody;

    private Object request;
    private Object response;
    
    private String serviceName;
    private String inputName;
    private String publisherType;

    private String customString1;
    private String customString2;
    private String customString3;
    private String customString4;
    private String customString5;

    private Long customNumber1;
    private Long customNumber2;
    private Long customNumber3;
    private Long customNumber4;
    private Long customNumber5;

    private Date customDate1;
    private Date customDate2;
    private Date customDate3;

    private String url;
    
    public LoggingRecord() {
    }
    
    public LoggingRecord(String id,
            Date incomingTime,
            Date outcomingTime,
            Object request,
            Object response,
            String requestBody,
            String responseBody,
            String serviceName,
            String url,
            String inputName,
            String publisherType,
            String customString1,
            String customString2,
            String customString3,
            String customString4,
            String customString5,
            Long customNumber1,
            Long customNumber2,
            Long customNumber3,
            Long customNumber4,
            Long customNumber5,
            Date customDate1,
            Date customDate2,
            Date customDate3) {
        super();
        this.id = id;
        this.incomingTime = incomingTime;
        this.outcomingTime = outcomingTime;
        this.request = request;
        this.response = response;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.serviceName = serviceName;
        this.url = url;
        this.inputName = inputName;
        this.publisherType = publisherType;
        this.customString1 = customString1;
        this.customString2 = customString2;
        this.customString3 = customString3;
        this.customString4 = customString4;
        this.customString5 = customString5;
        this.customNumber1 = customNumber1;
        this.customNumber2 = customNumber2;
        this.customNumber3 = customNumber3;
        this.customNumber4 = customNumber4;
        this.customNumber5 = customNumber5;
        this.customDate1 = customDate1;
        this.customDate2 = customDate2;
        this.customDate3 = customDate3;
    }

    public String getPublisherType() {
        return publisherType;
    }

    public String getId() {
        return id;
    }

    public Object getRequest() {
        return request;
    }

    public Object getResponse() {
        return response;
    }

    public Date getIncomingTime() {
        return incomingTime;
    }

    public Date getOutcomingTime() {
        return outcomingTime;
    }

    public String getInputName() {
        return inputName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUrl() {
        return url;
    }

    public String getCustomString1() {
        return customString1;
    }

    public String getCustomString2() {
        return customString2;
    }

    public String getCustomString3() {
        return customString3;
    }

    public String getCustomString4() {
        return customString4;
    }

    public String getCustomString5() {
        return customString5;
    }

    public Long getCustomNumber1() {
        return customNumber1;
    }

    public Long getCustomNumber2() {
        return customNumber2;
    }

    public Long getCustomNumber3() {
        return customNumber3;
    }

    public Long getCustomNumber4() {
        return customNumber4;
    }

    public Long getCustomNumber5() {
        return customNumber5;
    }

    public Date getCustomDate1() {
        return customDate1;
    }

    public Date getCustomDate2() {
        return customDate2;
    }

    public Date getCustomDate3() {
        return customDate3;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    @UseLoggingInfoConvertor(convertor = RandomUUID.class)
    public void setId(String id) {
        this.id = id;
    }

    @SetterIncomingTime
    public void setIncomingTime(Date incomingTime) {
        this.incomingTime = incomingTime;
    }

    @SetterOutcomingTime
    public void setOutcomingTime(Date outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    @SetterRequest
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    @SetterResponse
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    @UseLoggingInfoConvertor(convertor = JSONRequest.class, publisherTypes = PublisherType.RESTFUL)
    public void setRequest(Object request) {
        this.request = request;
    }

    @UseLoggingInfoConvertor(convertor = JSONResponse.class, publisherTypes = PublisherType.RESTFUL)
    public void setResponse(Object response) {
        this.response = response;
    }

    @SetterServiceName
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @SetterInputName
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    @SetterPublisher
    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    @SetterCustomStringValue1
    public void setCustomString1(String customString1) {
        this.customString1 = customString1;
    }

    @SetterCustomStringValue2
    public void setCustomString2(String customString2) {
        this.customString2 = customString2;
    }

    @SetterCustomStringValue3
    public void setCustomString3(String customString3) {
        this.customString3 = customString3;
    }

    @SetterCustomStringValue4
    public void setCustomString4(String customString4) {
        this.customString4 = customString4;
    }

    @SetterCustomStringValue5
    public void setCustomString5(String customString5) {
        this.customString5 = customString5;
    }

    @SetterCustomNumberValue1
    public void setCustomNumber1(Long customNumber1) {
        this.customNumber1 = customNumber1;
    }

    @SetterCustomNumberValue2
    public void setCustomNumber2(Long customNumber2) {
        this.customNumber2 = customNumber2;
    }

    @SetterCustomNumberValue3
    public void setCustomNumber3(Long customNumber3) {
        this.customNumber3 = customNumber3;
    }

    @SetterCustomNumberValue4
    public void setCustomNumber4(Long customNumber4) {
        this.customNumber4 = customNumber4;
    }

    @SetterCustomNumberValue5
    public void setCustomNumber5(Long customNumber5) {
        this.customNumber5 = customNumber5;
    }

    @SetterCustomDateValue1
    public void setCustomDate1(Date customDate1) {
        this.customDate1 = customDate1;
    }

    @SetterCustomDateValue2
    public void setCustomDate2(Date customDate2) {
        this.customDate2 = customDate2;
    }

    @SetterCustomDateValue3
    public void setCustomDate3(Date customDate3) {
        this.customDate3 = customDate3;
    }

    @SetterUrl
    public void setUrl(String url) {
        this.url = url;
    }
    
    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }
}
