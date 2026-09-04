package org.openl.codegen.tools.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TablePriorityRuleWrapper {
    @Getter
    private final String priorityRule;

    public static class SimplePriorityRuleWrapper extends TablePriorityRuleWrapper {
        @Getter
        private final String operationName;
        @Getter
        private final String propertyName;

        public SimplePriorityRuleWrapper(String priorityRule, String operationName, String propertyName) {
            super(priorityRule);
            this.operationName = operationName;
            this.propertyName = propertyName;
        }
    }

    public static class JavaClassPriorityRuleWrapper extends TablePriorityRuleWrapper {
        @Getter
        private final String className;

        public JavaClassPriorityRuleWrapper(String priorityRule, String className) {
            super(priorityRule);
            this.className = className;
        }
    }
}
