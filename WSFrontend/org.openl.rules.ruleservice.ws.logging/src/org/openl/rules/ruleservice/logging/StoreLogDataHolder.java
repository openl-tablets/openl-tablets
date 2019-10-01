package org.openl.rules.ruleservice.logging;

/**
 * Thread save singleton holder for LoggingInfo. Required for storing logging data to external resource feature.
 *
 * @author Marat Kamalov
 *
 */
public final class StoreLogDataHolder {

    private StoreLogDataHolder() {
    }

    public static final ThreadLocal<StoreLogData> RULESERVICE_LOGGING_HOLDER = new ThreadLocal<>();

    public static StoreLogData get() {
        StoreLogData storeLogData = RULESERVICE_LOGGING_HOLDER.get();
        if (storeLogData == null) {
            storeLogData = new StoreLogData();
            RULESERVICE_LOGGING_HOLDER.set(storeLogData);
        }
        return storeLogData;
    }

    public static void remove() {
        RULESERVICE_LOGGING_HOLDER.remove();
    }
}
