package org.openl.rules.validation.properties.dimentional;

import org.apache.commons.lang3.StringUtils;
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

    private static final String LOGICAL_OR = " || ";

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
        MatchingExpression matchExpression = getProperty().getExpression();        
        String result = getMatchByDefaultCodeExpression(matchExpression);        
        
        // array values can have only "contains" operation  
        //
        StringBuilder codeExpression = new StringBuilder();
        
        if (matchExpression != null) {
            
            if (getNumberOfLocalParameters() == 1) {
                // code expression will look like: "<propertyName>Local == <contextValue>"
                //
                result += createCodeExpression(matchExpression, getLocalParameterName());
            } else {
                // building condition like: 
                // "<propertyName>Local1 == <contextValue> || <propertyName>Local2 == <contextValue> || ..."
                //
                for (int i = 1; i <= getNumberOfLocalParameters(); i++) {
                    if (i > 1){
                        codeExpression.append(LOGICAL_OR);
                    }
                    String parameterName = (getLocalParameterName() + i).intern();
                    codeExpression.append(createCodeExpression(matchExpression, parameterName));
                }
                result += codeExpression.toString();
            }
        } else {
            String message = String.format("Can`t create expression for \"%s\" property validation.", 
                getProperty().getName());
            OpenLMessagesUtils.addWarn(message);
        }
        return result.intern();
    }

    public String getTitle() {        
        return getProperty().getDisplayName();
    }

    public String getParameterDeclaration() {
        Class<?> componentType = getProperty().getType().getInstanceClass().getComponentType();
        final String simpleName = componentType.getSimpleName();
        final String localParameterName = getLocalParameterName();
        return new StringBuilder(64).append(simpleName).append(' ').append(localParameterName).toString().intern();
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
    
    private String getLocalParameterName() {
        return (getProperty().getName() + ADispatcherTableColumn.LOCAL_PARAM_SUFFIX).intern();
    }
    
    private String createCodeExpression(MatchingExpression matchExpression, String parameterName) {        
        return matchExpression.getMatchExpression().getCodeExpression(parameterName);
    }

}
