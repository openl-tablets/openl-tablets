package org.openl.rules.ruleservice.logging;

/**
 * Thread save singleton holder for LoggingInfo. Required for storing logging data to external resource feature.
 *
 * @author Marat Kamalov
 *
 */
public final class StoreLoggingDataHolder {

    private StoreLoggingDataHolder() {
    }

    public static final ThreadLocal<StoreLoggingData> RULESERVICE_LOGGING_HOLDER = new ThreadLocal<>();

    public static StoreLoggingData get() {
        StoreLoggingData requestData = RULESERVICE_LOGGING_HOLDER.get();
        if (requestData == null) {
            requestData = new StoreLoggingData();
            RULESERVICE_LOGGING_HOLDER.set(requestData);
        }
        return requestData;
    }

    public static void remove() {
        RULESERVICE_LOGGING_HOLDER.remove();
    }
}
