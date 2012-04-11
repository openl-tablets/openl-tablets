package org.openl.meta.explanation;

import java.util.Iterator;

import org.openl.meta.number.NumberFormula;
import org.openl.meta.number.NumberValue;
import org.openl.meta.number.NumberValue.ValueType;

/**
* Explanation implementation for number values that are of type {@link ValueType#FORMULA}, see also
* {@link NumberValue#getValueType()}.
* 
* @author DLiauchuk
*
* @param <T> type that extends {@link ExplanationNumberValue} 
*/
public class FormulaExplanationValue<T extends ExplanationNumberValue<T>> extends SingleValueExplanation<T> {
    
    private NumberFormula<T> formulaHolder;
    
    public FormulaExplanationValue(NumberFormula<T> formulaHolder) {
        this.formulaHolder = formulaHolder;
    }
    
    @Override
    public Iterator<T> getChildren() {
        return formulaHolder.getArguments().iterator();
    }
    
    @Override
    public String getType() {
        return String.format("%s.%s", ValueType.FORMULA, formulaHolder.getOperand());
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

}
