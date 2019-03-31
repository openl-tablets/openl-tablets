package org.openl.rules.ruleservice.logging.elasticsearch;

import java.util.UUID;

import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoConvertor;

public class RandomUUID implements LoggingInfoConvertor<String> {
    @Override
    public String convert(LoggingInfo loggingInfo) {
        return UUID.randomUUID().toString();
    }
}
