package org.openl.rules.ruleservice.logging.cassandra;

import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoStoringService;
import org.openl.rules.ruleservice.logging.cassandra.LoggingRecord.LoggingRecordBuilder;
import org.springframework.data.cassandra.core.CassandraOperations;

import com.datastax.driver.core.utils.UUIDs;

public class CassandraLoggingInfoStoreService implements LoggingInfoStoringService {

    private CassandraOperations cassandraOperations;

    public CassandraOperations getCassandraOperations() {
        return cassandraOperations;
    }

    public void setCassandraOperations(CassandraOperations cassandraOperations) {
        this.cassandraOperations = cassandraOperations;
    }

    @Override
    public void store(LoggingInfo loggingInfo) {
        String publisherType = null;
        if (loggingInfo.getPublisherType() != null) {
            publisherType = loggingInfo.getPublisherType().toString();
        }
        LoggingRecordBuilder loggingRecordBuilder = new LoggingRecordBuilder();
        loggingRecordBuilder.setId(UUIDs.timeBased().toString());
        loggingRecordBuilder.setIncomingTime(loggingInfo.getIncomingMessageTime());
        loggingRecordBuilder.setOutcomingTime(loggingInfo.getOutcomingMessageTime());
        loggingRecordBuilder.setRequest(loggingInfo.getRequestMessage().getPayload().toString());
        loggingRecordBuilder.setResponse(loggingInfo.getResponseMessage().getPayload().toString());
        loggingRecordBuilder.setServiceName(loggingInfo.getServiceName());
        loggingRecordBuilder.setUrl(loggingInfo.getRequestMessage().getAddress().toString());
        loggingRecordBuilder.setInputName(loggingInfo.getInputName());
        loggingRecordBuilder.setPublisherType(publisherType);

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

        cassandraOperations.insert(loggingRecord);
    }
}
