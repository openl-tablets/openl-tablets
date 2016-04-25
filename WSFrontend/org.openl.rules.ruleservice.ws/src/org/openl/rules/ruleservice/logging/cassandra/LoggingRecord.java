package org.openl.rules.ruleservice.logging.cassandra;

import java.util.Date;

import org.springframework.data.cassandra.mapping.PrimaryKey;
import org.springframework.data.cassandra.mapping.Table;

@Table
public class LoggingRecord {
    @PrimaryKey
    private String id;

    private Date incomingTime;
    private Date outcomingTime;

    private String request;
    private String response;

    private String serviceName;
    private String url;
    private String inputName;
    private String publisherType;

    private String customString1;
    private String customString2;
    private String customString3;
    private String customString4;
    private String customString5;

    private Integer customNumber1;
    private Integer customNumber2;
    private Integer customNumber3;
    private Integer customNumber4;
    private Integer customNumber5;

    private Date customDate1;
    private Date customDate2;
    private Date customDate3;

    public LoggingRecord(String id,
            Date incomingTime,
            Date outcomingTime,
            String request,
            String response,
            String serviceName,
            String url,
            String inputName,
            String publisherType,
            String customString1,
            String customString2,
            String customString3,
            String customString4,
            String customString5,
            Integer customNumber1,
            Integer customNumber2,
            Integer customNumber3,
            Integer customNumber4,
            Integer customNumber5,
            Date customDate1,
            Date customDate2,
            Date customDate3) {
        super();
        this.id = id;
        this.incomingTime = incomingTime;
        this.outcomingTime = outcomingTime;
        this.request = request;
        this.response = response;
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

    public String getRequest() {
        return request;
    }

    public String getResponse() {
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

    public Integer getCustomNumber1() {
        return customNumber1;
    }

    public Integer getCustomNumber2() {
        return customNumber2;
    }

    public Integer getCustomNumber3() {
        return customNumber3;
    }

    public Integer getCustomNumber4() {
        return customNumber4;
    }

    public Integer getCustomNumber5() {
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

    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }
}
