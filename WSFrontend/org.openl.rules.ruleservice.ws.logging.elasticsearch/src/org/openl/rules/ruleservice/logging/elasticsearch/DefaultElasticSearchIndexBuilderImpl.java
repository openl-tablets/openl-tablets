package org.openl.rules.ruleservice.logging.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import org.eclipse.jetty.util.ajax.JSON;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.logging.LoggingInfo;

public class DefaultElasticSearchIndexBuilderImpl implements ElasticSearchIndexBuilder {
    private static final String ID = "DEFAULT_ELASTIC_SEARCH_INDEX_ID";

    @Override
    public LoggingRecord withObject(LoggingInfo loggingInfo) {
        Object request = null;
        Object response = null;
        if (RulesDeploy.PublisherType.RESTFUL.equals(loggingInfo.getPublisherType())) {
            request = JSON.parse(loggingInfo.getRequestMessage().getPayload().toString());
            response = JSON.parse(loggingInfo.getResponseMessage().getPayload().toString());
        }
        String id = null;

        Object existingId = loggingInfo.getContext().get(ID);
        if (existingId != null) {
            id = (String) existingId;
        } else {
            id = UUID.randomUUID().toString();
            loggingInfo.getContext().put(ID, id);
        } 
        String publisherType = null;
        if (loggingInfo.getPublisherType() != null) {
            publisherType = loggingInfo.getPublisherType().toString();
        }

        LoggingRecord.LoggingRecordBuilder loggingRecordBuilder = new LoggingRecord.LoggingRecordBuilder();
        loggingRecordBuilder.setId(id);
        loggingRecordBuilder.setIncomingTime(loggingInfo.getIncomingMessageTime());
        loggingRecordBuilder.setOutcomingTime(loggingInfo.getOutcomingMessageTime());
        loggingRecordBuilder.setRequest(request);
        loggingRecordBuilder.setResponse(response);
        loggingRecordBuilder.setServiceName(loggingInfo.getServiceName());
        loggingRecordBuilder.setUrl(loggingInfo.getRequestMessage().getAddress().toString());
        loggingRecordBuilder.setInputName(loggingInfo.getInputName());
        loggingRecordBuilder.setPublisherType(publisherType);
        loggingRecordBuilder.setRequestBody(loggingInfo.getRequestMessage().getPayload().toString());
        loggingRecordBuilder.setResponseBody(loggingInfo.getResponseMessage().getPayload().toString());

        if (loggingInfo.getLoggingCustomData() != null) {
            loggingRecordBuilder.setStringValue1(loggingInfo.getLoggingCustomData().getStringValue1());
            loggingRecordBuilder.setStringValue2(loggingInfo.getLoggingCustomData().getStringValue2());
            loggingRecordBuilder.setStringValue3(loggingInfo.getLoggingCustomData().getStringValue3());
            loggingRecordBuilder.setStringValue4(loggingInfo.getLoggingCustomData().getStringValue4());
            loggingRecordBuilder.setStringValue5(loggingInfo.getLoggingCustomData().getStringValue5());
            loggingRecordBuilder.setNumberValue1(loggingInfo.getLoggingCustomData().getNumberValue1());
            loggingRecordBuilder.setNumberValue2(loggingInfo.getLoggingCustomData().getNumberValue2());
            loggingRecordBuilder.setNumberValue3(loggingInfo.getLoggingCustomData().getNumberValue3());
            loggingRecordBuilder.setNumberValue4(loggingInfo.getLoggingCustomData().getNumberValue4());
            loggingRecordBuilder.setNumberValue5(loggingInfo.getLoggingCustomData().getNumberValue5());
            loggingRecordBuilder.setDateValue1(loggingInfo.getLoggingCustomData().getDateValue1());
            loggingRecordBuilder.setDateValue2(loggingInfo.getLoggingCustomData().getDateValue2());
            loggingRecordBuilder.setDateValue3(loggingInfo.getLoggingCustomData().getDateValue3());
        }

        LoggingRecord loggingRecord = loggingRecordBuilder.build();
       
        return loggingRecord;
    }

    @Override
    public String withId(LoggingInfo loggingInfo) {
        String id = null;

        Object existingId = loggingInfo.getContext().get(ID);
        if (existingId != null) {
            id = (String) existingId;
        } else {
            id = UUID.randomUUID().toString();
            loggingInfo.getContext().put(ID, id);
        }
        return id;
    }

    @Override
    public String withIndexName(LoggingInfo loggingInfo) {
        try{
            return URLEncoder.encode(loggingInfo.getServiceName(), "UTF-8").toLowerCase();
        }catch(UnsupportedEncodingException e){
            return null;
        }
    }

    @Override
    public String withType(LoggingInfo loggingInfo) {
        return null;
    }

    @Override
    public String withSource(LoggingInfo loggingInfo) {
        return null;
    }

    @Override
    public String withParentId(LoggingInfo loggingInfo) {
        return null;
    }

    @Override
    public Long withVersion(LoggingInfo loggingInfo) {
        return null;
    }
}
