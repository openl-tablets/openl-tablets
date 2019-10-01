package org.openl.rules.ruleservice.logging.advice;

import java.util.Map;

/**
 * Before advice for logging method calls.
 *
 * @author Marat Kamalov
 *
 */
public interface StoreLogDataAdvice {
    void prepare(Map<String, Object> values, Object[] args, Object result, Exception ex);
}
