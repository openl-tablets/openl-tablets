package org.openl.rules.ruleservice.logging.elasticsearch;

import org.eclipse.jetty.util.ajax.JSON;
import org.openl.rules.ruleservice.logging.StoreLogData;
import org.openl.rules.ruleservice.logging.StoreLogDataConvertor;

public class JSONResponse implements StoreLogDataConvertor<Object> {
    @Override
    public Object convert(StoreLogData storeLogData) {
        if (storeLogData.getResponseMessage() != null && storeLogData.getResponseMessage().getPayload() != null) {
            return JSON.parse(storeLogData.getResponseMessage().getPayload().toString());
        }
        return null;
    }
}
