package org.openl.rules.validation.properties.dimentional;

import org.apache.commons.lang.StringUtils;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;

/**
 * Column that is used in the dispatching table, built by dimension properties of the group of tables.
 * Handles the column with array dimension property.
 * 
 * @author DLiauchuk
 *
 */
public class ArrayParameterColumn extends ADispatcherTableColumn {
    
    public ArrayParameterColumn(TablePropertyDefinition arrayDimensionProperty, 
            DispatcherTableRules rules) {
        super(arrayDimensionProperty, rules);
        validate();
    }
    
    private void validate() {
        if (!getProperty().getType().getInstanceClass().isArray()) {
            throw new OpenlNotCheckedException("Can`t create array parameter column for not an array property");
        }
    }

    public String getCodeExpression() {
        String propertyName = getProperty().getName();
        MatchingExpression matchExpression = getProperty().getExpression();        
        
        // array values can have only "contains" operation  
        //
        StringBuffer codeExpression = new StringBuffer();
        
        if (matchExpression != null) {
            // building condition like: 
            // "<propertyName>Local1 == <contextValue> || <propertyName>Local2 == <contextValue> || ..."
            //
            for (int i = 1; i <= getNumberOfLocalParameters(); i++) {
                if (i > 1){
                    codeExpression.append(" || ");
                }
                String oneValueName = String.format("%s%s%d", propertyName, 
                    ADispatcherTableColumn.LOCAL_PARAM_SUFFIX, i);
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
        return getProperty().getDisplayName();
    }

    public String getParameterDeclaration() {
        Class<?> componentType = getProperty().getType().getInstanceClass().getComponentType();
        return String.format("%s %s", componentType.getSimpleName(), getProperty().getName() + 
            ADispatcherTableColumn.LOCAL_PARAM_SUFFIX);        
    }
    
    
    public String getRuleValue(int ruleIndex, int localParameterIndex) {
        String valuesThroughComma = getRules().getRule(ruleIndex).getPropertyValueAsString(getProperty().getName());
        String[] values = StringUtils.split(valuesThroughComma, ",");
        if (values != null && (values.length > localParameterIndex)) {
            return values[localParameterIndex];
        } else {
            return null;
        }
    }
    
    public int getNumberOfLocalParameters() {
        int maxNumberOfValues = 0;
        
        for (int i = 0; i < getRules().getRulesNumber(); i++) {
            // as the the property is an array, so its value is array
            //
            Object[] values = (Object[])getRules().getRule(i).getPropertyValue(getProperty().getName());
            
            // find the max number of values from all rules
            //
            if (values != null) {
                int numberOfValues = values.length;
                if (numberOfValues > maxNumberOfValues) {
                    maxNumberOfValues = numberOfValues;
                }
            }
        }        
        return maxNumberOfValues;        
    }
}
