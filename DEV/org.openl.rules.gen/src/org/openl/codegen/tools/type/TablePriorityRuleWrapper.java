package org.openl.codegen.tools.type;

import lombok.Getter;

public class TablePriorityRuleWrapper {

    public static class SimplePriorityRuleWrapper extends TablePriorityRuleWrapper {
        @Getter
        private final String operationName;
        @Getter
        private final String propertyName;

        public SimplePriorityRuleWrapper(String operationName, String propertyName) {
            this.operationName = operationName;
            this.propertyName = propertyName;
        }
    }

    public static class JavaClassPriorityRuleWrapper extends TablePriorityRuleWrapper {
        @Getter
        private final String className;

        public JavaClassPriorityRuleWrapper(String className) {
            this.className = className;
        }
    }
}
