package org.openl.rules.ruleservice.logging.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoMapper;

public class DefaultIndexBuilderImpl implements IndexBuilder {
    private static final String ID = "DEFAULT_ELASTIC_SEARCH_INDEX_ID";

    LoggingInfoMapper loggingInfoMapper = new LoggingInfoMapper();
    
    @Override
    public LoggingRecord withObject(LoggingInfo loggingInfo) {
        LoggingRecord loggingRecord = new LoggingRecord();
        
        loggingInfoMapper.map(loggingInfo, loggingRecord);
       
        return loggingRecord;
    }

    @Override
    public String withId(LoggingInfo loggingInfo) {
        String id = null;

        Object existingId = loggingInfo.getLoggingContext().get(ID);
        if (existingId != null) {
            id = (String) existingId;
        } else {
            id = UUID.randomUUID().toString();
            loggingInfo.getLoggingContext().put(ID, id);
        }
        return id;
    }

    @Override
    public String withIndexName(LoggingInfo loggingInfo) {
        try {
            return URLEncoder.encode(loggingInfo.getServiceName(), "UTF-8").toLowerCase();
        } catch (UnsupportedEncodingException e) {
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
