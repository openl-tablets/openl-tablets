package org.openl.rules.ruleservice.logging.advice;

import java.util.Map;

/**
 * Before advice for logging method calls.
 *
 * @author Marat Kamalov
 *
 */
public interface StoreLoggingAdvice {
    void populateCustomData(Map<String, Object> customValues, Object[] args, Object result, Exception ex);
}
