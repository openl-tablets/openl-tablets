package org.openl.rules.ruleservice.logging.cassandra;

import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoStoringService;
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
    public void store(LoggingInfo loggingData) {
        String publisherType = null;
        if (loggingData.getPublisherType() != null) {
            publisherType = loggingData.getPublisherType().toString();
        }
        LoggingRecord loggingRecord;
        if (loggingData.getLoggingCustomData() != null) {
            loggingRecord = new LoggingRecord(UUIDs.timeBased().toString(),
                loggingData.getIncomingMessageTime(),
                loggingData.getOutcomingMessageTime(),
                loggingData.getRequestMessage().getPayload().toString(),
                loggingData.getResponseMessage().getPayload().toString(),
                loggingData.getService().getName(),
                loggingData.getRequestMessage().getAddress().toString(),
                loggingData.getInputName(),
                publisherType,
                loggingData.getLoggingCustomData().getCustomString1(),
                loggingData.getLoggingCustomData().getCustomString2(),
                loggingData.getLoggingCustomData().getCustomString3(),
                loggingData.getLoggingCustomData().getCustomString4(),
                loggingData.getLoggingCustomData().getCustomString5(),
                loggingData.getLoggingCustomData().getCustomNumber1(),
                loggingData.getLoggingCustomData().getCustomNumber2(),
                loggingData.getLoggingCustomData().getCustomNumber3(),
                loggingData.getLoggingCustomData().getCustomNumber4(),
                loggingData.getLoggingCustomData().getCustomNumber5(),
                loggingData.getLoggingCustomData().getCustomDate1(),
                loggingData.getLoggingCustomData().getCustomDate2(),
                loggingData.getLoggingCustomData().getCustomDate3());
        } else {
            loggingRecord = new LoggingRecord(UUIDs.timeBased().toString(),
                loggingData.getIncomingMessageTime(),
                loggingData.getOutcomingMessageTime(),
                loggingData.getRequestMessage().getPayload().toString(),
                loggingData.getResponseMessage().getPayload().toString(),
                loggingData.getService().getName(),
                loggingData.getRequestMessage().getAddress().toString(),
                loggingData.getInputName(),
                publisherType,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        }
        cassandraOperations.insert(loggingRecord);
    }
}
