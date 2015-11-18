package org.openl.rules.ruleservice.logging;

/**
 * Thread save sinleton holder for LoggingInfo. Requred for storing logging data
 * to external resource feature.
 * 
 * @author Marat Kamalov
 *
 */
public class LoggingInfoHolder {
    public static final ThreadLocal<LoggingInfo> requestDataHolder = new ThreadLocal<LoggingInfo>();

    public static LoggingInfo get() {
        LoggingInfo requestData = requestDataHolder.get();
        if (requestData == null) {
            requestData = new LoggingInfo();
            requestDataHolder.set(requestData);
        }
        return requestData;
    }

    public static void remove() {
        requestDataHolder.remove();
    }
}
