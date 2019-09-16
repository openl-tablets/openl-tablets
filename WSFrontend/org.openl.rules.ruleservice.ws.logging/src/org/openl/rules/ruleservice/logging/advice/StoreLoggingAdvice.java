package org.openl.rules.ruleservice.logging.advice;

import org.openl.rules.ruleservice.logging.CustomData;

/**
 * Before advice for logging method calls.
 *
 * @author Marat Kamalov
 *
 */
public interface StoreLoggingAdvice {
    void populateCustomData(CustomData customData,
            Object[] args,
            Object result,
            Exception ex);
}
