package org.openl.codegen.tools.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

@RequiredArgsConstructor
public class TablePriorityRuleWrapper {
    @Getter
    private final String priorityRule;

    public static class SimplePriorityRuleWrapper extends TablePriorityRuleWrapper {
        @Getter
        private final String operationName;
        @Getter
        private final String propertyName;
        private final Class<?> propertyType;

        public SimplePriorityRuleWrapper(String priorityRule, String operationName, String propertyName) {
            super(priorityRule);
            this.operationName = operationName;
            this.propertyName = propertyName;
            propertyType = TablePropertyDefinitionUtils.getPropertyTypeByPropertyName(propertyName);
        }

        public String getPropertyType() {
            return propertyType.getName();
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
