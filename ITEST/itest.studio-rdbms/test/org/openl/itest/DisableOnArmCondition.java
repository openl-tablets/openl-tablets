package org.openl.itest;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;

class DisableOnArmCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(org.junit.jupiter.api.extension.ExtensionContext context) {
        String arch = System.getProperty("os.arch");
        if (arch.contains("aarch") || arch.contains("arm")) {
            return ConditionEvaluationResult.disabled("Disabled on ARM architecture");
        }
        return ConditionEvaluationResult.enabled("Enabled on non-ARM architecture");

    }
}
