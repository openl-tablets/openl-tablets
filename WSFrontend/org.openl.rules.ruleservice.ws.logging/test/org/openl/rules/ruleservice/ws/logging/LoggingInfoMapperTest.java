package org.openl.rules.ruleservice.ws.logging;

import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.cxf.interceptor.LoggingMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.logging.LoggingCustomData;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoConvertor;
import org.openl.rules.ruleservice.logging.LoggingInfoMapper;
import org.openl.rules.ruleservice.logging.RuleServiceLogging;
import org.openl.rules.ruleservice.logging.TypeConvertor;
import org.openl.rules.ruleservice.logging.annotation.SetterIncomingTime;
import org.openl.rules.ruleservice.logging.annotation.SetterInputName;
import org.openl.rules.ruleservice.logging.annotation.SetterOutcomingTime;
import org.openl.rules.ruleservice.logging.annotation.SetterPublisher;
import org.openl.rules.ruleservice.logging.annotation.SetterRequest;
import org.openl.rules.ruleservice.logging.annotation.SetterResponse;
import org.openl.rules.ruleservice.logging.annotation.SetterServiceName;
import org.openl.rules.ruleservice.logging.annotation.SetterUrl;
import org.openl.rules.ruleservice.logging.annotation.SetterValue;
import org.openl.rules.ruleservice.logging.annotation.UseLoggingInfoConvertor;

public class LoggingInfoMapperTest {

    private static final String SOME_VALUE = RandomStringUtils.random(10, true, true);

    private long beginTime;
    private long endTime;

    @Before
    public void setUp() {
        beginTime = Timestamp.valueOf("1980-01-01 00:00:00").getTime();
        endTime = Timestamp.valueOf("2020-12-31 00:58:00").getTime();
    }

    /**
     * Method should generate random number that represents a time between two dates.
     * 
     * @return
     */
    private Date getRandomTimeBetweenTwoDates() {
        long diff = endTime - beginTime + 1;
        long d = beginTime + (long) (Math.random() * diff);
        return new Date(d);
    }

    @Test
    public void testPublisherFilteringMapping() {
        LoggingInfoMapper mapper = new LoggingInfoMapper();

        RuleServiceLogging ruleServiceLoggingInfo = new RuleServiceLogging();
        final String customString1 = RandomStringUtils.random(10, true, true);
        final String customString2 = RandomStringUtils.random(10, true, true);

        LoggingCustomData loggingCustomData = new LoggingCustomData();
        loggingCustomData.setValue("customString1", customString1);
        loggingCustomData.setValue("customString2", customString2);

        ruleServiceLoggingInfo.setLoggingCustomData(loggingCustomData);

        final PublisherType publisher1 = PublisherType.RESTFUL;
        ruleServiceLoggingInfo.setPublisherType(publisher1);
        LoggingInfo loggingInfo = new LoggingInfo(ruleServiceLoggingInfo);

        TestEntity testEntity1 = new TestEntity();
        mapper.map(loggingInfo, testEntity1);

        // validation
        Assert.assertEquals(customString2, testEntity1.getValue2());
        Assert.assertEquals(customString2, testEntity1.getStringValue2());
        Assert.assertEquals(null, testEntity1.getValue1());

        final PublisherType publisher2 = PublisherType.WEBSERVICE;
        ruleServiceLoggingInfo.setPublisherType(publisher2);

        TestEntity testEntity2 = new TestEntity();
        mapper.map(loggingInfo, testEntity2);

        // validation
        Assert.assertEquals(null, testEntity2.getValue2());
        Assert.assertEquals(customString1, testEntity2.getValue1());
    }

    @Test
    public void testPublisherConvertorMapping() {
        LoggingInfoMapper mapper = new LoggingInfoMapper();

        RuleServiceLogging ruleServiceLoggingInfo = new RuleServiceLogging();
        final String customString1 = RandomStringUtils.random(10, true, true);

        LoggingCustomData loggingCustomData = new LoggingCustomData();
        loggingCustomData.setValue("customString1", " " + customString1 + " ");

        ruleServiceLoggingInfo.setLoggingCustomData(loggingCustomData);

        final PublisherType publisher1 = PublisherType.RESTFUL;
        ruleServiceLoggingInfo.setPublisherType(publisher1);
        LoggingInfo loggingInfo = new LoggingInfo(ruleServiceLoggingInfo);

        TestEntity testEntity = new TestEntity();
        mapper.map(loggingInfo, testEntity);

        // validation
        Assert.assertEquals(customString1, testEntity.getValue3());
    }

    @Test
    public void testSimpleMapping() {
        RuleServiceLogging ruleServiceLoggingInfo = new RuleServiceLogging();

        final String customString1 = RandomStringUtils.random(10, true, true);
        final String customString2 = RandomStringUtils.random(10, true, true);
        final String customString3 = RandomStringUtils.random(10, true, true);

        LoggingCustomData loggingCustomData = new LoggingCustomData();
        loggingCustomData.setValue("customString1", customString1);
        loggingCustomData.setValue("customString2", customString2);
        loggingCustomData.setValue("customString3", customString3);

        ruleServiceLoggingInfo.setLoggingCustomData(loggingCustomData);

        final String request = RandomStringUtils.random(10);
        final String response = RandomStringUtils.random(10);
        final String inputName = RandomStringUtils.random(10);
        final String url = RandomStringUtils.random(10);
        final PublisherType publisher = PublisherType.RESTFUL;
        final String serviceName = RandomStringUtils.random(10);

        final Date incomingMessageTime = getRandomTimeBetweenTwoDates();
        final Date outcomingMessageTime = getRandomTimeBetweenTwoDates();

        ruleServiceLoggingInfo.setIncomingMessageTime(incomingMessageTime);
        ruleServiceLoggingInfo.setOutcomingMessageTime(outcomingMessageTime);
        ruleServiceLoggingInfo.setInputName(inputName);
        ruleServiceLoggingInfo.setServiceName(serviceName);
        ruleServiceLoggingInfo.setPublisherType(publisher);
        LoggingMessage requestLoggingMessage = new LoggingMessage("", "");
        requestLoggingMessage.getPayload().append(request);
        requestLoggingMessage.getAddress().append(url);
        ruleServiceLoggingInfo.setRequestMessage(requestLoggingMessage);

        LoggingMessage responseLoggingMessage = new LoggingMessage("", "");
        responseLoggingMessage.getPayload().append(response);
        responseLoggingMessage.getAddress().append(url);
        ruleServiceLoggingInfo.setResponseMessage(responseLoggingMessage);

        LoggingInfo loggingInfo = new LoggingInfo(ruleServiceLoggingInfo);

        LoggingInfoMapper mapper = new LoggingInfoMapper();
        TestEntity testEntity = new TestEntity();

        mapper.map(loggingInfo, testEntity);

        // validation
        Assert.assertEquals(SOME_VALUE, testEntity.getId());
        Assert.assertEquals(inputName, testEntity.getInputName());
        Assert.assertEquals(incomingMessageTime, testEntity.getIncomingTime());
        Assert.assertEquals(outcomingMessageTime, testEntity.getOutcomingTime());
        Assert.assertEquals(serviceName, testEntity.getServiceName());
        Assert.assertEquals(publisher.toString(), testEntity.getPublisherType());
        Assert.assertEquals(url, url);
        Assert.assertEquals(request, testEntity.getRequest());
        Assert.assertEquals(response, testEntity.getResponse());

        // Custom data
        Assert.assertEquals(customString1, testEntity.getStringValue1());
        Assert.assertEquals(customString2, testEntity.getValue2());
        Assert.assertEquals(customString2, testEntity.getStringValue2());
        Assert.assertEquals(customString3, testEntity.getStringValue3());
    }

    public static class SomeValueConvertor implements LoggingInfoConvertor<String> {
        @Override
        public String convert(LoggingInfo loggingInfo) {
            return SOME_VALUE;
        }
    }

    public static class TrimConvertor implements TypeConvertor<String, String> {
        @Override
        public String convert(String value) {
            if (value == null) {
                return null;
            }
            return value.trim();
        }
    }

    public static class TestEntity {
        private String id;
        private Date incomingTime;
        private Date outcomingTime;
        private String request;
        private String response;
        private String serviceName;
        private String url;
        private String inputName;
        private String publisherType;
        private String stringValue1;
        private String stringValue2;
        private String stringValue3;

        private String value1;
        private String value2;
        private String value3;

        public TestEntity() {
        }

        public String getId() {
            return id;
        }

        @UseLoggingInfoConvertor(convertor = SomeValueConvertor.class)
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

        @SetterValue("customString1")
        public void setStringValue1(String stringValue1) {
            this.stringValue1 = stringValue1;
        }

        public String getStringValue2() {
            return stringValue2;
        }

        @SetterValue("customString2")
        public void setStringValue2(String stringValue2) {
            this.stringValue2 = stringValue2;
        }

        public String getStringValue3() {
            return stringValue3;
        }

        @SetterValue("customString3")
        public void setStringValue3(String stringValue3) {
            this.stringValue3 = stringValue3;
        }

        @Override
        public String toString() {
            return "TestEntity [id=" + id + "]";
        }

        public String getValue1() {
            return value1;
        }

        @SetterValue(value = "customString1", publisherTypes = PublisherType.WEBSERVICE)
        public void setValue1(String value1) {
            this.value1 = value1;
        }

        public String getValue2() {
            return value2;
        }

        @SetterValue(value = "customString2", publisherTypes = PublisherType.RESTFUL)
        public void setValue2(String value2) {
            this.value2 = value2;
        }

        public String getValue3() {
            return value3;
        }

        @SetterValue(value = "customString1", convertor = TrimConvertor.class)
        public void setValue3(String value3) {
            this.value3 = value3;
        }

    }
}
