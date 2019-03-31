package org.openl.rules.ruleservice.core;

public final class RuleServiceStaticConfigurationUtil {

    private RuleServiceStaticConfigurationUtil() {
    }

    public static final int MAX_THREADS_FOR_COMPILE = 3;

    private static int maxThreadsForCompile = MAX_THREADS_FOR_COMPILE;

    public static int getMaxThreadsForCompile() {
        return maxThreadsForCompile;
    }

    public static void setMaxThreadsForCompile(int maxThreadsForCompile) {
        RuleServiceStaticConfigurationUtil.maxThreadsForCompile = maxThreadsForCompile;
    }
}
