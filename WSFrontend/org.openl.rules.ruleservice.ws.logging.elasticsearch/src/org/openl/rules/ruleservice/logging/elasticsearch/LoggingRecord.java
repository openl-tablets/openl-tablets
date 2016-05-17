package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "loggingrecord")
public class LoggingRecord {
    @Id
    private String id;

    private Date incomingTime;
    private Date outcomingTime;

    private Object request;
    private Object response;

    private String serviceName;
    private String url;
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

    public LoggingRecord(String id,
            Date incomingTime,
            Date outcomingTime,
            Object request,
            Object response,
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

    @Override
    public String toString() {
        return "LoggingRecord [id=" + id + "]";
    }

    public static class LoggingRecordBuilder {
        protected String id;

        protected Date incomingTime;
        protected Date outcomingTime;

        protected Object request;
        protected Object response;

        protected String serviceName;
        protected String url;
        protected String inputName;
        protected String publisherType;

        protected String stringValue1;
        protected String stringValue2;
        protected String stringValue3;
        protected String stringValue4;
        protected String stringValue5;

        protected Long numberValue1;
        protected Long numberValue2;
        protected Long numberValue3;
        protected Long numberValue4;
        protected Long numberValue5;

        protected Date dateValue1;
        protected Date dateValue2;
        protected Date dateValue3;

        public LoggingRecord build() {
            return new LoggingRecord(id,
                incomingTime,
                outcomingTime,
                request,
                response,
                serviceName,
                url,
                inputName,
                publisherType,
                stringValue1,
                stringValue2,
                stringValue3,
                stringValue4,
                stringValue5,
                numberValue1,
                numberValue2,
                numberValue3,
                numberValue4,
                numberValue5,
                dateValue1,
                dateValue2,
                dateValue3);
        }

        public LoggingRecordBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public LoggingRecordBuilder setIncomingTime(Date incomingTime) {
            this.incomingTime = incomingTime;
            return this;
        }

        public LoggingRecordBuilder setOutcomingTime(Date outcomingTime) {
            this.outcomingTime = outcomingTime;
            return this;
        }

        public LoggingRecordBuilder setRequest(Object request) {
            this.request = request;
            return this;
        }

        public LoggingRecordBuilder setResponse(Object response) {
            this.response = response;
            return this;
        }

        public LoggingRecordBuilder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public LoggingRecordBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public LoggingRecordBuilder setInputName(String inputName) {
            this.inputName = inputName;
            return this;
        }

        public LoggingRecordBuilder setPublisherType(String publisherType) {
            this.publisherType = publisherType;
            return this;
        }

        public LoggingRecordBuilder setStringValue1(String stringValue1) {
            this.stringValue1 = stringValue1;
            return this;
        }

        public LoggingRecordBuilder setStringValue2(String stringValue2) {
            this.stringValue2 = stringValue2;
            return this;
        }

        public LoggingRecordBuilder setStringValue3(String stringValue3) {
            this.stringValue3 = stringValue3;
            return this;
        }

        public LoggingRecordBuilder setStringValue4(String stringValue4) {
            this.stringValue4 = stringValue4;
            return this;
        }

        public LoggingRecordBuilder setStringValue5(String stringValue5) {
            this.stringValue5 = stringValue5;
            return this;
        }

        public LoggingRecordBuilder setNumberValue1(Long numberValue1) {
            this.numberValue1 = numberValue1;
            return this;
        }

        public LoggingRecordBuilder setNumberValue2(Long numberValue2) {
            this.numberValue2 = numberValue2;
            return this;
        }

        public LoggingRecordBuilder setNumberValue3(Long numberValue3) {
            this.numberValue3 = numberValue3;
            return this;
        }

        public LoggingRecordBuilder setNumberValue4(Long numberValue4) {
            this.numberValue4 = numberValue4;
            return this;
        }

        public LoggingRecordBuilder setNumberValue5(Long numberValue5) {
            this.numberValue5 = numberValue5;
            return this;
        }

        public LoggingRecordBuilder setDateValue1(Date dateValue1) {
            this.dateValue1 = dateValue1;
            return this;
        }

        public LoggingRecordBuilder setDateValue2(Date dateValue2) {
            this.dateValue2 = dateValue2;
            return this;
        }

        public String getId() {
            return id;
        }

        public Date getIncomingTime() {
            return incomingTime;
        }

        public Date getOutcomingTime() {
            return outcomingTime;
        }

        public Object getRequest() {
            return request;
        }

        public Object getResponse() {
            return response;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getUrl() {
            return url;
        }

        public String getInputName() {
            return inputName;
        }

        public String getPublisherType() {
            return publisherType;
        }

        public String getStringValue1() {
            return stringValue1;
        }

        public String getStringValue2() {
            return stringValue2;
        }

        public String getStringValue3() {
            return stringValue3;
        }

        public String getStringValue4() {
            return stringValue4;
        }

        public String getStringValue5() {
            return stringValue5;
        }

        public Long getNumberValue1() {
            return numberValue1;
        }

        public Long getNumberValue2() {
            return numberValue2;
        }

        public Long getNumberValue3() {
            return numberValue3;
        }

        public Long getNumberValue4() {
            return numberValue4;
        }

        public Long getNumberValue5() {
            return numberValue5;
        }

        public Date getDateValue1() {
            return dateValue1;
        }

        public Date getDateValue2() {
            return dateValue2;
        }

        public Date getDateValue3() {
            return dateValue3;
        }

        public LoggingRecordBuilder setDateValue3(Date dateValue3) {
            this.dateValue3 = dateValue3;
            return this;
        }
    }
}
