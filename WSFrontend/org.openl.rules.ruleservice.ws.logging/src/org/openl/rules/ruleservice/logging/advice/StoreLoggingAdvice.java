package org.openl.rules.ruleservice.logging.advice;

import org.openl.rules.ruleservice.logging.LoggingCustomData;

/**
 * Before advice for logging method calls.
 *
 * @author Marat Kamalov
 *
 */
public interface StoreLoggingAdvice {
    LoggingCustomData populateCustomData(LoggingCustomData loggingCustomData,
            Object[] args,
            Object result,
            Exception ex);
}
