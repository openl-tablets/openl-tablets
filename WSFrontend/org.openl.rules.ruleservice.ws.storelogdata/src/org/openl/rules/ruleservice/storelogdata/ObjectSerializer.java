package org.openl.rules.ruleservice.storelogdata;

public interface ObjectSerializer {

    String writeValueAsString(Object obj) throws ProcessingException;

    <T> T readValue(String content, Class<T> type) throws ProcessingException;

}
