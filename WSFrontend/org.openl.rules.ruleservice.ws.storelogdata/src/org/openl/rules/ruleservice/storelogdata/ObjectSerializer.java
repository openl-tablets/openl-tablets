package org.openl.rules.ruleservice.storelogdata;

public interface ObjectSerializer {

    String writeValueAsString(Object obj) throws ProcessingException;

}
