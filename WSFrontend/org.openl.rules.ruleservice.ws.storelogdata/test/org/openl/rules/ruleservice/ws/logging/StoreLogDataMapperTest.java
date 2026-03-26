package org.openl.rules.ruleservice.ws.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.cxf.interceptor.LoggingMessage;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.storelogdata.Converter;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
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

public class StoreLogDataMapperTest {

    private static final String SOME_VALUE = RandomStringUtils.random(10, true, true);

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
        assertEquals(customString2, testEntity1.getValue2());
        assertEquals(customString2, testEntity1.getStringValue2());
        assertNull(testEntity1.getValue1());

        final PublisherType publisher2 = PublisherType.WEBSERVICE;
        storeLogData.setPublisherType(publisher2);

        TestEntity testEntity2 = new TestEntity();
        mapper.map(storeLogData, testEntity2);

        // validation
        assertNull(testEntity2.getValue2());
        assertEquals(customString1, testEntity2.getValue1());
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
        assertEquals(customString1, testEntity.getValue3());
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

        final ZonedDateTime incomingMessageTime = ZonedDateTime.now();
        final ZonedDateTime outcomingMessageTime = ZonedDateTime.now().plus(1, ChronoUnit.MINUTES);

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
        assertEquals(SOME_VALUE, testEntity.getId());
        assertEquals("testSimpleMapping", testEntity.getMethodName());
        assertEquals(incomingMessageTime, testEntity.getIncomingTime());
        assertEquals(outcomingMessageTime, testEntity.getOutcomingTime());
        assertEquals(serviceName, testEntity.getServiceName());
        assertEquals(publisher.toString(), testEntity.getPublisherType());
        assertEquals(url, testEntity.getUrl());
        assertEquals(request, testEntity.getRequest());
        assertEquals(response, testEntity.getResponse());

        // Custom data
        assertEquals(customString1, testEntity.getStringValue1());
        assertEquals(customString2, testEntity.getValue2());
        assertEquals(customString2, testEntity.getStringValue2());
        assertEquals(customString3, testEntity.getStringValue3());
    }

    public static class SomeValueConvertor implements Converter<StoreLogData, String> {
        @Override
        public String apply(StoreLogData value) {
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

    @NoArgsConstructor
    public static class TestEntity {
        @Getter
        @Setter(onMethod_ = {@Value(converter = SomeValueConvertor.class)})
        private String id;
        @Getter
        @Setter(onMethod_ = {@IncomingTime})
        private ZonedDateTime incomingTime;
        @Getter
        @Setter(onMethod_ = {@OutcomingTime})
        private ZonedDateTime outcomingTime;
        @Getter(onMethod_ = {@Request})
        @Setter
        private String request;
        @Getter
        @Setter(onMethod_ = {@Response})
        private String response;
        @Getter(onMethod_ = {@ServiceName})
        @Setter
        private String serviceName;
        @Getter
        @Setter
        @Url
        private String url;
        @Getter
        @MethodName
        @Setter
        private String methodName;
        @Getter
        @Setter(onMethod_ = {@Publisher})
        private String publisherType;
        @Getter
        @Setter(onMethod_ = {@Value("customString1")})
        private String stringValue1;
        @Getter
        @Setter(onMethod_ = {@Value("customString2")})
        private String stringValue2;
        @Getter
        @Setter(onMethod_ = {@Value("customString3")})
        private String stringValue3;

        @Getter
        @Value(value = "customString1")
        @QualifyPublisherType(org.openl.rules.ruleservice.storelogdata.annotation.PublisherType.WEBSERVICE)
        @Setter
        private String value1;
        @Getter(onMethod_ = {@Value(value = "customString2"), @QualifyPublisherType(org.openl.rules.ruleservice.storelogdata.annotation.PublisherType.RESTFUL)})
        @Setter
        private String value2;
        @Getter
        @Setter
        @Value(value = "customString1", converter = TrimConvertor.class)
        private String value3;

        @Override
        public String toString() {
            return "TestEntity [id=" + id + "]";
        }

    }
}
