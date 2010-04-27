package org.openl.rules.validation.properties.dimentional;

import org.apache.commons.lang.StringUtils;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;

public class ArrayDimensionPropertyColumn extends ADimensionPropertyColumn {
    
    private TablePropertyDefinition property;
    private DimensionPropertiesRules rules;
    
    public ArrayDimensionPropertyColumn(TablePropertyDefinition property, DimensionPropertiesRules rules) {
        this.property = property;
        this.rules = rules;
    }
    
    public String getCodeExpression() {
        String propertyName = property.getName();
        MatchingExpression matchExpression = property.getExpression();        
        
        // array values can have only "contains" operation        
        StringBuffer codeExpression = new StringBuffer();
        
        if (matchExpression != null) {
            // building condition like "<propertyName>Local1 == <contextValue> || <propertyName>Local2 == <contextValue> || ..."
            for (int i = 1; i <= getMaxNumberOfValuesForRules(); i++) {
                if (i > 1){
                    codeExpression.append(" || ");
                }
                String oneValueName = String.format("%s%s%d", propertyName, ADimensionPropertyColumn.LOCAL_PARAM_SUFFIX, i);
                String expressionForOneValue = matchExpression.getMatchExpression().getCodeExpression(oneValueName);
                codeExpression.append(expressionForOneValue);
            }
        } else {
            String message = String.format("Can`t create expression for \"%s\" property validation.", propertyName);
            OpenLMessagesUtils.addWarn(message);
        }
        return codeExpression.toString();        
    }

    public String getTitle() {        
        return property.getDisplayName();
    }

    public String getParameterDeclaration() {
        Class<?> componentType = property.getType().getInstanceClass().getComponentType();
        return String.format("%s %s", componentType.getSimpleName(), property.getName() + ADimensionPropertyColumn.LOCAL_PARAM_SUFFIX);        
    }
    
    public String getRuleValue(int ruleIndex) {
        return getRuleValue(ruleIndex, 0);
    }
    
    public String getRuleValue(int ruleIndex, int elementNum) {
        String valuesThroughComma = rules.getRule(ruleIndex).getPropertyValueAsString(property.getName());
        String[] values = StringUtils.split(valuesThroughComma, ",");
        if (values != null && (values.length > elementNum)) {
            return values[elementNum];
        } else {
            return null;
        }
    }
    
    public boolean isArrayCondition() {        
        return true;
    }
    
    public int getMaxNumberOfValuesForRules() {
        int maxNumberOfArrayValues = 0;
        
        for (int i = 0; i < rules.getRulesNumber(); i++) {
            Object[] values = (Object[])rules.getRule(i).getPropertyValue(property.getName());
            if (values != null) {
                int numberOfValues = values.length;
                if (numberOfValues > maxNumberOfArrayValues) {
                    maxNumberOfArrayValues = numberOfValues;
                }
            }
        }        
        return maxNumberOfArrayValues;        
    }
}
