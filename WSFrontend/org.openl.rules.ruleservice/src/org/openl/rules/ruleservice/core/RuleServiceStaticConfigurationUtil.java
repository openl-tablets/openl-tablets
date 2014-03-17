package org.openl.rules.ruleservice.core;

public class RuleServiceStaticConfigurationUtil {
    public final static int MAX_THREADS_FOR_COMPILE = 3;

    public static int maxThreadsForCompile = MAX_THREADS_FOR_COMPILE;

    public static int getMaxThreadsForCompile() {
        return maxThreadsForCompile;
    }
    
    public static void setMaxThreadsForCompile(int maxThreadsForCompile) {
        RuleServiceStaticConfigurationUtil.maxThreadsForCompile = maxThreadsForCompile;
    }
}
