package org.openl.codegen.tools.type;

import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

public class TablePriorityRuleWrapper {
    private String priorityRule;

    public TablePriorityRuleWrapper(String priorityRule) {
        this.priorityRule = priorityRule;
    }

    public String getPriorityRule() {
        return priorityRule;
    }

    public static class SimplePriorityRuleWrapper extends TablePriorityRuleWrapper {
        private String operationName;
        private String propertyName;
        private Class<?> propertyType;

        public SimplePriorityRuleWrapper(String priorityRule, String operationName, String propertyName) {
            super(priorityRule);
            this.operationName = operationName;
            this.propertyName = propertyName;
            propertyType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propertyName);
        }

        public String getOperationName() {
            return operationName;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getPropertyType() {
            return propertyType.getName();
        }
    }

    public static class JavaClassPriorityRuleWrapper extends TablePriorityRuleWrapper {
        private String className;

        public JavaClassPriorityRuleWrapper(String priorityRule, String className) {
            super(priorityRule);
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }
}
