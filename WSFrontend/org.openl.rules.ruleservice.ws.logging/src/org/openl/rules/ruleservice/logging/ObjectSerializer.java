package org.openl.rules.ruleservice.logging;

public interface ObjectSerializer {
    
    String writeValueAsString(Object obj) throws ProcessingException;
    
}
