package org.openl.rules.ruleservice.logging.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingDataMapper;

public class DefaultIndexBuilderImpl implements IndexBuilder {
    private static final String ID = "DEFAULT_ELASTIC_SEARCH_INDEX_ID";

    StoreLoggingDataMapper storeLoggingDataMapper = new StoreLoggingDataMapper();

    @Override
    public DefaultElasticEntity withObject(StoreLoggingData storeLoggingData) {
        DefaultElasticEntity loggingRecord = new DefaultElasticEntity();

        storeLoggingDataMapper.map(storeLoggingData, loggingRecord);

        return loggingRecord;
    }

    @Override
    public String withId(StoreLoggingData storeLoggingData) {
        String id = null;

        Object existingId = storeLoggingData.getCustomValues().get(ID);
        if (existingId != null) {
            id = (String) existingId;
        } else {
            id = UUID.randomUUID().toString();
            storeLoggingData.getCustomValues().put(ID, id);
        }
        return id;
    }

    @Override
    public String withIndexName(StoreLoggingData storeLoggingData) {
        try {
            return URLEncoder.encode(storeLoggingData.getServiceName(), "UTF-8").toLowerCase();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public String withType(StoreLoggingData storeLoggingData) {
        return null;
    }

    @Override
    public String withSource(StoreLoggingData storeLoggingData) {
        return null;
    }

    @Override
    public String withParentId(StoreLoggingData storeLoggingData) {
        return null;
    }

    @Override
    public Long withVersion(StoreLoggingData storeLoggingData) {
        return null;
    }
}
