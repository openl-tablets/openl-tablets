package org.openl.rules.ruleservice.logging.elasticsearch;

import org.eclipse.jetty.util.ajax.JSON;
import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingConvertor;

public class JSONRequest implements StoreLoggingConvertor<Object> {
    @Override
    public Object convert(StoreLoggingData storeLoggingData) {
        return JSON.parse(storeLoggingData.getRequestMessage().getPayload().toString());
    }
}
