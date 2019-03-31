package org.openl.rules.ruleservice.logging.elasticsearch;

import org.eclipse.jetty.util.ajax.JSON;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoConvertor;

public class JSONRequest implements LoggingInfoConvertor<Object> {
    @Override
    public Object convert(LoggingInfo loggingInfo) {
        return JSON.parse(loggingInfo.getRequestMessage().getPayload().toString());
    }
}
