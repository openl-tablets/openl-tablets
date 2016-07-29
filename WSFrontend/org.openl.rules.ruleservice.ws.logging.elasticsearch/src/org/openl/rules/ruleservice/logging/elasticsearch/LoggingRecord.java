package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.Date;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
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
import org.openl.rules.ruleservice.logging.annotation.UseLoggingInfo;
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

    @UseLoggingInfo(convertor = RandomUUID.class)
    public void setId(String id) {
        this.id = id;
    }

    @IncomingTime
    public void setIncomingTime(Date incomingTime) {
        this.incomingTime = incomingTime;
    }

    @OutcomingTime
    public void setOutcomingTime(Date outcomingTime) {
        this.outcomingTime = outcomingTime;
    }

    @Request
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    @Response
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    @UseLoggingInfo(convertor = JSONRequest.class, publisherTypes = PublisherType.RESTFUL)
    public void setRequest(Object request) {
        this.request = request;
    }

    @UseLoggingInfo(convertor = JSONResponse.class, publisherTypes = PublisherType.RESTFUL)
    public void setResponse(Object response) {
        this.response = response;
    }

    @ServiceName
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @InputName
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    @Publisher
    public void setPublisherType(String publisherType) {
        this.publisherType = publisherType;
    }

    @CustomStringValue1
    public void setCustomString1(String customString1) {
        this.customString1 = customString1;
    }

    @CustomStringValue2
    public void setCustomString2(String customString2) {
        this.customString2 = customString2;
    }

    @CustomStringValue3
    public void setCustomString3(String customString3) {
        this.customString3 = customString3;
    }

    @CustomStringValue4
    public void setCustomString4(String customString4) {
        this.customString4 = customString4;
    }

    @CustomStringValue5
    public void setCustomString5(String customString5) {
        this.customString5 = customString5;
    }

    @CustomNumberValue1
    public void setCustomNumber1(Long customNumber1) {
        this.customNumber1 = customNumber1;
    }

    @CustomNumberValue2
    public void setCustomNumber2(Long customNumber2) {
        this.customNumber2 = customNumber2;
    }

    @CustomNumberValue3
    public void setCustomNumber3(Long customNumber3) {
        this.customNumber3 = customNumber3;
    }

    @CustomNumberValue4
    public void setCustomNumber4(Long customNumber4) {
        this.customNumber4 = customNumber4;
    }

    @CustomNumberValue5
    public void setCustomNumber5(Long customNumber5) {
        this.customNumber5 = customNumber5;
    }

    @CustomDateValue1
    public void setCustomDate1(Date customDate1) {
        this.customDate1 = customDate1;
    }

    @CustomDateValue2
    public void setCustomDate2(Date customDate2) {
        this.customDate2 = customDate2;
    }

    @CustomDateValue3
    public void setCustomDate3(Date customDate3) {
        this.customDate3 = customDate3;
    }

    @Url
    public void setUrl(String url) {
        this.url = url;
    }
    
    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }
}
