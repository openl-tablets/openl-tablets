package org.openl.rules.ruleservice.logging;

/**
 * Thread save sinleton holder for LoggingInfo. Requred for storing logging data
 * to external resource feature.
 * 
 * @author Marat Kamalov
 *
 */
public final class RuleServiceLoggingInfoHolder {
    public static final ThreadLocal<RuleServiceLoggingInfo> ruleServiceLoggingInfoHolder = new ThreadLocal<RuleServiceLoggingInfo>();

    public static RuleServiceLoggingInfo get() {
        RuleServiceLoggingInfo requestData = ruleServiceLoggingInfoHolder.get();
        if (requestData == null) {
            requestData = new RuleServiceLoggingInfo();
            ruleServiceLoggingInfoHolder.set(requestData);
        }
        return requestData;
    }

    public static void remove() {
        ruleServiceLoggingInfoHolder.remove();
    }
}
