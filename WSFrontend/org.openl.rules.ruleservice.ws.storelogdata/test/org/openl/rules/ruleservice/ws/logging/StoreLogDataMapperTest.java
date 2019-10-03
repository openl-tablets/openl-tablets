package org.openl.rules.ruleservice.ws.logging;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.cxf.interceptor.LoggingMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.storelogdata.Converter;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataConverter;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.annotation.IncomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.MethodName;
import org.openl.rules.ruleservice.storelogdata.annotation.OutcomingTime;
import org.openl.rules.ruleservice.storelogdata.annotation.Publisher;
import org.openl.rules.ruleservice.storelogdata.annotation.QualifyPublisherType;
import org.openl.rules.ruleservice.storelogdata.annotation.Request;
import org.openl.rules.ruleservice.storelogdata.annotation.Response;
import org.openl.rules.ruleservice.storelogdata.annotation.ServiceName;
import org.openl.rules.ruleservice.storelogdata.annotation.Url;
import org.openl.rules.ruleservice.storelogdata.annotation.Value;
import org.openl.rules.ruleservice.storelogdata.annotation.WithStoreLogDataConverter;

public class StoreLogDataMapperTest {

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
        StoreLogDataMapper mapper = new StoreLogDataMapper();

        StoreLogData storeLogData = new StoreLogData();
        final String customString1 = RandomStringUtils.random(10, true, true);
        final String customString2 = RandomStringUtils.random(10, true, true);

        Map<String, Object> customValues = storeLogData.getCustomValues();
        customValues.put("customString1", customString1);
        customValues.put("customString2", customString2);

        final PublisherType publisher1 = PublisherType.RESTFUL;
        storeLogData.setPublisherType(publisher1);

        TestEntity testEntity1 = new TestEntity();
        mapper.map(storeLogData, testEntity1);

        // validation
        Assert.assertEquals(customString2, testEntity1.getValue2());
        Assert.assertEquals(customString2, testEntity1.getStringValue2());
        Assert.assertEquals(null, testEntity1.getValue1());

        final PublisherType publisher2 = PublisherType.WEBSERVICE;
        storeLogData.setPublisherType(publisher2);

        TestEntity testEntity2 = new TestEntity();
        mapper.map(storeLogData, testEntity2);

        // validation
        Assert.assertEquals(null, testEntity2.getValue2());
        Assert.assertEquals(customString1, testEntity2.getValue1());
    }

    @Test
    public void testPublisherConvertorMapping() {
        StoreLogDataMapper mapper = new StoreLogDataMapper();

        StoreLogData storeLogData = new StoreLogData();
        final String customString1 = RandomStringUtils.random(10, true, true);

        Map<String, Object> customValues = storeLogData.getCustomValues();
        customValues.put("customString1", " " + customString1 + " ");

        final PublisherType publisher1 = PublisherType.RESTFUL;
        storeLogData.setPublisherType(publisher1);

        TestEntity testEntity = new TestEntity();
        mapper.map(storeLogData, testEntity);

        // validation
        Assert.assertEquals(customString1, testEntity.getValue3());
    }

    @Test
    public void testSimpleMapping() throws Exception {
        StoreLogData storeLogData = new StoreLogData();

        final String customString1 = RandomStringUtils.random(10, true, true);
        final String customString2 = RandomStringUtils.random(10, true, true);
        final String customString3 = RandomStringUtils.random(10, true, true);

        Map<String, Object> customValues = storeLogData.getCustomValues();
        customValues.put("customString1", customString1);
        customValues.put("customString2", customString2);
        customValues.put("customString3", customString3);

        final String request = RandomStringUtils.random(10);
        final String response = RandomStringUtils.random(10);
        storeLogData.setServiceMethod(StoreLogDataMapperTest.class.getMethod("testSimpleMapping"));
        final String url = RandomStringUtils.random(10);
        final PublisherType publisher = PublisherType.RESTFUL;
        final String serviceName = RandomStringUtils.random(10);

        final Date incomingMessageTime = getRandomTimeBetweenTwoDates();
        final Date outcomingMessageTime = getRandomTimeBetweenTwoDates();

        storeLogData.setIncomingMessageTime(incomingMessageTime);
        storeLogData.setOutcomingMessageTime(outcomingMessageTime);
        storeLogData.setServiceName(serviceName);
        storeLogData.setPublisherType(publisher);
        LoggingMessage requestLoggingMessage = new LoggingMessage("", "");
        requestLoggingMessage.getPayload().append(request);
        requestLoggingMessage.getAddress().append(url);
        storeLogData.setRequestMessage(requestLoggingMessage);

        LoggingMessage responseLoggingMessage = new LoggingMessage("", "");
        responseLoggingMessage.getPayload().append(response);
        responseLoggingMessage.getAddress().append(url);
        storeLogData.setResponseMessage(responseLoggingMessage);

        StoreLogDataMapper mapper = new StoreLogDataMapper();
        TestEntity testEntity = new TestEntity();

        mapper.map(storeLogData, testEntity);

        // validation
        Assert.assertEquals(SOME_VALUE, testEntity.getId());
        Assert.assertEquals("testSimpleMapping", testEntity.getMethodName());
        Assert.assertEquals(incomingMessageTime, testEntity.getIncomingTime());
        Assert.assertEquals(outcomingMessageTime, testEntity.getOutcomingTime());
        Assert.assertEquals(serviceName, testEntity.getServiceName());
        Assert.assertEquals(publisher.toString(), testEntity.getPublisherType());
        Assert.assertEquals(url, testEntity.getUrl());
        Assert.assertEquals(request, testEntity.getRequest());
        Assert.assertEquals(response, testEntity.getResponse());

        // Custom data
        Assert.assertEquals(customString1, testEntity.getStringValue1());
        Assert.assertEquals(customString2, testEntity.getValue2());
        Assert.assertEquals(customString2, testEntity.getStringValue2());
        Assert.assertEquals(customString3, testEntity.getStringValue3());
    }

    public static class SomeValueConvertor implements StoreLogDataConverter<String> {
        @Override
        public String convert(StoreLogData storeLogData) {
            return SOME_VALUE;
        }
    }

    public static class TrimConvertor implements Converter<String, String> {
        @Override
        public String apply(String value) {
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
        @Url
        private String url;
        @MethodName
        private String methodName;
        private String publisherType;
        private String stringValue1;
        private String stringValue2;
        private String stringValue3;

        @Value(value = "customString1")
        @QualifyPublisherType(org.openl.rules.ruleservice.storelogdata.annotation.PublisherType.WEBSERVICE)
        private String value1;
        private String value2;
        @Value(value = "customString1", converter = TrimConvertor.class)
        private String value3;

        public TestEntity() {
        }

        public String getId() {
            return id;
        }

        @WithStoreLogDataConverter(converter = SomeValueConvertor.class)
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

        @Request
        public String getRequest() {
            return request;
        }

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

        @ServiceName
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

        public void setMethodName(String inputName) {
            this.methodName = inputName;
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

        @Value("customString1")
        public void setStringValue1(String stringValue1) {
            this.stringValue1 = stringValue1;
        }

        public String getStringValue2() {
            return stringValue2;
        }

        @Value("customString2")
        public void setStringValue2(String stringValue2) {
            this.stringValue2 = stringValue2;
        }

        public String getStringValue3() {
            return stringValue3;
        }

        @Value("customString3")
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

        public void setValue1(String value1) {
            this.value1 = value1;
        }

        @Value(value = "customString2")
        @QualifyPublisherType(org.openl.rules.ruleservice.storelogdata.annotation.PublisherType.RESTFUL)
        public String getValue2() {
            return value2;
        }

        public void setValue2(String value2) {
            this.value2 = value2;
        }

        public String getValue3() {
            return value3;
        }

        public void setValue3(String value3) {
            this.value3 = value3;
        }

    }
}
