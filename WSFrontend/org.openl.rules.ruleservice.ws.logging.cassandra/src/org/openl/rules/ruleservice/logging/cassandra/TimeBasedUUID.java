package org.openl.rules.ruleservice.logging.cassandra;

import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.LoggingInfoConvertor;

import com.datastax.driver.core.utils.UUIDs;

public class TimeBasedUUID implements LoggingInfoConvertor<String> {
    @Override
    public String convert(LoggingInfo loggingInfo) {
        return UUIDs.timeBased().toString();
    }
}
