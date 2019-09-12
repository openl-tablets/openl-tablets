package org.openl.rules.ruleservice.logging;

/**
 * Thread save singleton holder for LoggingInfo. Required for storing logging data to external resource feature.
 *
 * @author Marat Kamalov
 *
 */
public final class RuleServiceStoreLoggingDataolder {

    private RuleServiceStoreLoggingDataolder() {
    }

    public static final ThreadLocal<RuleServiceStoreLoggingData> RULESERVICE_LOGGING_HOLDER = new ThreadLocal<>();

    public static RuleServiceStoreLoggingData get() {
        RuleServiceStoreLoggingData requestData = RULESERVICE_LOGGING_HOLDER.get();
        if (requestData == null) {
            requestData = new RuleServiceStoreLoggingData();
            RULESERVICE_LOGGING_HOLDER.set(requestData);
        }
        return requestData;
    }

    public static void remove() {
        RULESERVICE_LOGGING_HOLDER.remove();
    }
}
