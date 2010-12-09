package org.openl.meta.explanation;

import java.util.Iterator;

import org.openl.meta.number.NumberFormula;
import org.openl.meta.number.NumberValue;
import org.openl.meta.number.NumberValue.ValueType;
import org.openl.util.OpenIterator;

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
    public String getDisplayName(int mode) {
        switch (mode) {
            case SHORT:
                return super.getDisplayName(mode);
            default:
                return super.getDisplayName(mode) + " = " + formulaHolder.getV1().getDisplayName(mode - 1) + formulaHolder.getOperand()
                        + formulaHolder.getV2().getDisplayName(mode - 1);
        }
    }
    
    @Override
    public String printContent(int mode, boolean fromMultiplicativeExpr, boolean inBrackets) {
        if ((mode & EXPAND_FORMULA) == 0) {
            return super.printContent(mode, fromMultiplicativeExpr, inBrackets);
        }

        StringBuffer res = new StringBuffer();
        if (!inBrackets) {
            if (!formulaHolder.isMultiplicative() && fromMultiplicativeExpr || ((mode & PRINT_VALUE_IN_EXPANDED) != 0)
                    && formulaHolder.isMultiplicative() && !fromMultiplicativeExpr) {
                res.append('(');
            }
        }

        if (((mode & PRINT_VALUE_IN_EXPANDED) != 0) && formulaHolder.isMultiplicative() && !fromMultiplicativeExpr) {
            res.append(printValue() + "=");
        }

        res.append(formulaHolder.getV1().printExplanationLocal(mode, formulaHolder.isMultiplicative()));
        res.append(formulaHolder.getOperand());
        res.append(formulaHolder.getV2().printExplanationLocal(mode, formulaHolder.isMultiplicative()));

        if (!inBrackets) {
            if (!formulaHolder.isMultiplicative() && fromMultiplicativeExpr || ((mode & PRINT_VALUE_IN_EXPANDED) != 0)
                    && formulaHolder.isMultiplicative() && !fromMultiplicativeExpr) {
                res.append(')');
            }
        }
        return res.toString();
    }
    
    @Override
    public Iterator<T> getChildren() {
        return OpenIterator.fromArray(formulaHolder.getArguments());
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
