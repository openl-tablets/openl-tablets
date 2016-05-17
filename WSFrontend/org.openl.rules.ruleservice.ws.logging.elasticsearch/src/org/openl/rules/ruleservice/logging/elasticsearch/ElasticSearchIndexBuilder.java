package org.openl.rules.ruleservice.logging.elasticsearch;

import org.openl.rules.ruleservice.logging.LoggingInfo;

public interface ElasticSearchIndexBuilder {
    LoggingRecord withObject(LoggingInfo loggingInfo);
    
    String withSource(LoggingInfo loggingInfo);
    
    String withId(LoggingInfo loggingInfo);
    
    String withParentId(LoggingInfo loggingInfo);
    
    String withIndexName(LoggingInfo loggingInfo);
    
    String withType(LoggingInfo loggingInfo);
    
    Long withVersion(LoggingInfo loggingInfo);
}
