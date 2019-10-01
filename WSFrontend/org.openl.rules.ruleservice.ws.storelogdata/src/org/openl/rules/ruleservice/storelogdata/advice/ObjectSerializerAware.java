package org.openl.rules.ruleservice.storelogdata.advice;

import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;

public interface ObjectSerializerAware {
    void setObjectSerializer(ObjectSerializer objectSerializer);
}
