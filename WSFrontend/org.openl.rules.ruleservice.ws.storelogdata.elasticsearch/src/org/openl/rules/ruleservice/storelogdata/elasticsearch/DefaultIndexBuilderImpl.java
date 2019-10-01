package org.openl.rules.ruleservice.storelogdata.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.IndexBuilder;

public class DefaultIndexBuilderImpl implements IndexBuilder {
    private static final String ID = "DEFAULT_ELASTIC_SEARCH_INDEX_ID";

    StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();

    @Override
    public DefaultElasticEntity withObject(StoreLogData storeLogData) {
        DefaultElasticEntity entity = new DefaultElasticEntity();

        storeLogDataMapper.map(storeLogData, entity);

        return entity;
    }

    @Override
    public String withId(StoreLogData storeLogData) {
        String id = null;

        Object existingId = storeLogData.getCustomValues().get(ID);
        if (existingId != null) {
            id = (String) existingId;
        } else {
            id = UUID.randomUUID().toString();
            storeLogData.getCustomValues().put(ID, id);
        }
        return id;
    }

    @Override
    public String withIndexName(StoreLogData storeLogData) {
        try {
            return URLEncoder.encode(storeLogData.getServiceName(), "UTF-8").toLowerCase();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public String withType(StoreLogData storeLogData) {
        return null;
    }

    @Override
    public String withSource(StoreLogData storeLogData) {
        return null;
    }

    @Override
    public String withParentId(StoreLogData storeLogData) {
        return null;
    }

    @Override
    public Long withVersion(StoreLogData storeLogData) {
        return null;
    }
}
