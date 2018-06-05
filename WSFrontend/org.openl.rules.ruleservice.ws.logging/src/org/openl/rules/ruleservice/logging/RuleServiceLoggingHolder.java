package org.openl.rules.ruleservice.logging;

/**
 * Thread save singleton holder for LoggingInfo. Required for storing logging data
 * to external resource feature.
 * 
 * @author Marat Kamalov
 *
 */
public final class RuleServiceLoggingHolder {
    public static final ThreadLocal<RuleServiceLogging> RULESERVICE_LOGGING_HOLDER = new ThreadLocal<RuleServiceLogging>();

    public static RuleServiceLogging get() {
        RuleServiceLogging requestData = RULESERVICE_LOGGING_HOLDER.get();
        if (requestData == null) {
            requestData = new RuleServiceLogging();
            RULESERVICE_LOGGING_HOLDER.set(requestData);
        }
        return requestData;
    }

    public static void remove() {
        RULESERVICE_LOGGING_HOLDER.remove();
    }
}
