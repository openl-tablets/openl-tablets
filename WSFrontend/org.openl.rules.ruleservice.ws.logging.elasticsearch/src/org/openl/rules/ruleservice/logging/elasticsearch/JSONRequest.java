package org.openl.rules.ruleservice.logging.elasticsearch;

import org.eclipse.jetty.util.ajax.JSON;
import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingDataConvertor;

public class JSONRequest implements StoreLoggingDataConvertor<Object> {
    @Override
    public Object convert(StoreLoggingData storeLoggingData) {
        if (storeLoggingData.getRequestMessage() != null && storeLoggingData.getRequestMessage().getPayload() != null) {
            return JSON.parse(storeLoggingData.getRequestMessage().getPayload().toString());
        }
        return null;
    }
}
