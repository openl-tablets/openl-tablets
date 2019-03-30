package org.openl.rules.validation.properties.dimentional;

import org.openl.exception.OpenlNotCheckedException;
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

    ArrayParameterColumn(TablePropertyDefinition arrayDimensionProperty,
            DispatcherTableRules rules) {
        super(arrayDimensionProperty, rules);
        validate();
    }
    
    private void validate() {
        if (!getProperty().getType().getInstanceClass().isArray()) {
            throw new OpenlNotCheckedException("Can`t create array parameter column for not an array property");
        }
    }

    @Override
    public String getCodeExpression() {
        MatchingExpression matchExpression = getProperty().getExpression();        
        String result = getMatchByDefaultCodeExpression(matchExpression);        
        
        // array values can have only "contains" operation  
        //
        StringBuilder codeExpression = new StringBuilder();
        
        if (matchExpression != null) {
            codeExpression.append("contains(");
            codeExpression.append(getLocalParameterName());
            codeExpression.append(",");
            codeExpression.append(matchExpression.getMatchExpression().getContextAttribute());
            codeExpression.append(")");
            result += codeExpression.toString();
        } else {
            String message = String.format("Can`t create expression for \"%s\" property validation.", 
                getProperty().getName());
            throw new OpenlNotCheckedException(message);
        }
        return result;
    }

    @Override
    public String getTitle() {        
        return getProperty().getDisplayName();
    }

    @Override
    public String getParameterDeclaration() {
        Class<?> componentType = getProperty().getType().getInstanceClass().getComponentType();
        return componentType.getSimpleName() + "[] " + getLocalParameterName();
    }
    
    @Override
    public String getRuleValue(int ruleIndex, int localParameterIndex) {
        return getRules().getRule(ruleIndex).getPropertyValueAsString(getProperty().getName());
    }
    
    private String getLocalParameterName() {
        return (getProperty().getName() + ADispatcherTableColumn.LOCAL_PARAM_SUFFIX).intern();
    }
}
