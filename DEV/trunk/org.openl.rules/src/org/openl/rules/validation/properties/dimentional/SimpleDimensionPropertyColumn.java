package org.openl.rules.validation.properties.dimentional;

import org.apache.commons.lang.StringUtils;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;

public class SimpleDimensionPropertyColumn extends ADimensionPropertyColumn {
    
    private DimensionPropertiesRules rules;
    private TablePropertyDefinition property;
    
    public SimpleDimensionPropertyColumn(TablePropertyDefinition property, DimensionPropertiesRules rules) {
        this.property = property;
        this.rules = rules;
    }
    
    public String getCodeExpression() {        
        return getCodeExpression(0);
    }
    
    public String getCodeExpression(int numberOfValues) {
        String result = StringUtils.EMPTY;
        
        String propertyName = property.getName();
        
        MatchingExpression matchExpression = property.getExpression();
        
        if (matchExpression != null) {
            result = matchExpression.getMatchExpression().getCodeExpression(propertyName + ADimensionPropertyColumn.LOCAL_PARAM_SUFFIX);
        } else {
            String message = String.format("Can`t create expression for \"%s\" property validation.", propertyName);
            OpenLMessagesUtils.addWarn(message);
        }
        return result;        
    }

    public String getTitle() {        
        return property.getDisplayName();
    }

    public String getParameterDeclaration() {        
        String propertyTypeName = property.getType().getInstanceClass().getSimpleName();
        return String.format("%s %s", propertyTypeName, property.getName() + ADimensionPropertyColumn.LOCAL_PARAM_SUFFIX);
    }
    
    public String getRuleValue(int ruleIndex, int elementNum) {        
        return rules.getRule(ruleIndex).getPropertyValueAsString(property.getName());
    }    
}
