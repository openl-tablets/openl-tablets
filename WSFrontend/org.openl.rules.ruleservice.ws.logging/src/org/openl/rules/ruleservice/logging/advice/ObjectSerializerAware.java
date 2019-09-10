package org.openl.rules.ruleservice.logging.advice;

import org.openl.rules.ruleservice.logging.ObjectSerializer;

public interface ObjectSerializerAware {
    void setObjectSerializer(ObjectSerializer objectSerializer);
}
