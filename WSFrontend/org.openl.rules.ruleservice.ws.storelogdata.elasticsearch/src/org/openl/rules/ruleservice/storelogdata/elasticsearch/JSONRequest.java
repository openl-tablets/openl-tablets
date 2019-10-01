package org.openl.rules.ruleservice.storelogdata.elasticsearch;

import org.eclipse.jetty.util.ajax.JSON;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataConvertor;

public class JSONRequest implements StoreLogDataConvertor<Object> {
    @Override
    public Object convert(StoreLogData storeLogData) {
        if (storeLogData.getRequestMessage() != null && storeLogData.getRequestMessage().getPayload() != null) {
            return JSON.parse(storeLogData.getRequestMessage().getPayload().toString());
        }
        return null;
    }
}
