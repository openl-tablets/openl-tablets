package org.openl.rules.ruleservice.logging.cassandra;

import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoStoringService;
import org.springframework.data.cassandra.core.CassandraOperations;

import com.datastax.driver.core.utils.UUIDs;

public class CassandraLoggingInfoStoreService implements LoggingInfoStoringService {

    //private final Logger log = LoggerFactory.getLogger(CassandraLoggingInfoStoreService.class);

    private CassandraOperations cassandraOperations;
    //private ObjectMapper jacksonObjectMapper;

    /*public ObjectMapper getJacksonObjectMapper() {
        return jacksonObjectMapper;
    }

    public void setJacksonObjectMapper(ObjectMapper jacksonObjectMapper) {
        this.jacksonObjectMapper = jacksonObjectMapper;
    }*/

    public CassandraOperations getCassandraOperations() {
        return cassandraOperations;
    }

    public void setCassandraOperations(CassandraOperations cassandraOperations) {
        this.cassandraOperations = cassandraOperations;
    }

    @Override
    public void store(LoggingInfo loggingData) {
        /*String jsonInputParameters = null;
        try {
            jsonInputParameters = jacksonObjectMapper.writeValueAsString(loggingData.getParameters());
        } catch (JsonProcessingException e) {
            log.error("Logging json input parameters failed!", e);
        }*/
        String publisherType = null;
        if (loggingData.getPublisherType() != null){
            publisherType = loggingData.getPublisherType().toString(); 
        }
        
        LoggingRecord loggingRecord = new LoggingRecord(UUIDs.timeBased().toString(),
            loggingData.getRequestMessage().getPayload().toString(),
            loggingData.getResponseMessage().getPayload().toString(),
            loggingData.getIncomingMessageTime(),
            loggingData.getOutcomingMessageTime(),
            loggingData.getService().getName(),
            loggingData.getRequestMessage().getAddress().toString(),
            loggingData.getInputName(),
            publisherType);
            //jsonInputParameters);
        cassandraOperations.insert(loggingRecord);
    }
}
