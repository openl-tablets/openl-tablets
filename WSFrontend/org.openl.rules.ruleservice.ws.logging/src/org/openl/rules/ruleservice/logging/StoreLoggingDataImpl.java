package org.openl.rules.ruleservice.logging;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.project.model.RulesDeploy.PublisherType;

public class StoreLoggingDataImpl implements StoreLoggingData {

    private RuleServiceStoreLoggingData ruleServiceStoreLoggingData;

    public StoreLoggingDataImpl(RuleServiceStoreLoggingData ruleServiceStoreLoggingData) {
        this.ruleServiceStoreLoggingData = ruleServiceStoreLoggingData;
    }

    public PublisherType getPublisherType() {
        return ruleServiceStoreLoggingData.getPublisherType();
    }

    public String getRequest() {
        switch (ruleServiceStoreLoggingData.getPublisherType()) {
            case KAFKA:
                return ruleServiceStoreLoggingData.getConsumerRecord().value().asText();
            case RMI:
                return null;
            case RESTFUL:
            case WEBSERVICE:
                if (ruleServiceStoreLoggingData.getRequestMessage() != null && ruleServiceStoreLoggingData
                    .getRequestMessage()
                    .getPayload() != null) {
                    return ruleServiceStoreLoggingData.getRequestMessage().getPayload().toString();
                }
        }
        return null;
    }

    public String getUrl() {
        if (ruleServiceStoreLoggingData.getRequestMessage() != null && ruleServiceStoreLoggingData.getRequestMessage()
            .getAddress() != null) {
            return ruleServiceStoreLoggingData.getRequestMessage().getAddress().toString();
        }
        return null;
    }

    public String getResponse() {
        switch (ruleServiceStoreLoggingData.getPublisherType()) {
            case KAFKA:
                return ruleServiceStoreLoggingData.getDltRecord() != null
                                                                          ? StringUtils.toEncodedString(
                                                                              ruleServiceStoreLoggingData.getDltRecord()
                                                                                  .value(),
                                                                              StandardCharsets.UTF_8)
                                                                          : null;
            case RMI:
                return null;
            case RESTFUL:
            case WEBSERVICE:
                if (ruleServiceStoreLoggingData.getResponseMessage() != null && ruleServiceStoreLoggingData
                    .getResponseMessage()
                    .getPayload() != null) {
                    return ruleServiceStoreLoggingData.getResponseMessage().getPayload().toString();
                }
        }
        return null;
    }

    public String getServiceName() {
        return ruleServiceStoreLoggingData.getServiceName();
    }

    public Date getIncomingMessageTime() {
        return ruleServiceStoreLoggingData.getIncomingMessageTime();
    }

    public Date getOutcomingMessageTime() {
        return ruleServiceStoreLoggingData.getOutcomingMessageTime();
    }

    public String getInputName() {
        return ruleServiceStoreLoggingData.getInputName();
    }

    public Object[] getParameters() {
        return ruleServiceStoreLoggingData.getParameters();
    }

    public CustomData getCustomData() {
        return ruleServiceStoreLoggingData.getCustomData();
    }

    public Method getServiceMethod() {
        return ruleServiceStoreLoggingData.getServiceMethod();
    }

    public String getOutTopic() {
        return ruleServiceStoreLoggingData.getOutTopic();
    }

    public String getInTopic() {
        return ruleServiceStoreLoggingData.getInTopic();
    }

    public boolean isIgnorable() {
        return ruleServiceStoreLoggingData.isIgnorable();
    }

}
