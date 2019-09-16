package org.openl.rules.ruleservice.logging.elasticsearch;

import org.eclipse.jetty.util.ajax.JSON;
import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingDataConvertor;

public class JSONResponse implements StoreLoggingDataConvertor<Object> {
    @Override
    public Object convert(StoreLoggingData storeLoggingData) {
        if (storeLoggingData.getResponseMessage() != null && storeLoggingData.getResponseMessage()
            .getPayload() != null) {
            return JSON.parse(storeLoggingData.getResponseMessage().getPayload().toString());
        }
        return null;
    }
}
