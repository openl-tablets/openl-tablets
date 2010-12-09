package org.openl.meta.explanation;

import java.util.Iterator;

import org.openl.meta.number.NumberFunction;
import org.openl.meta.number.NumberValue;
import org.openl.meta.number.NumberValue.ValueType;
import org.openl.util.OpenIterator;

/**
 * Explanation implementation for number values that are of type {@link ValueType#FUNCTION}, see also
 * {@link NumberValue#getValueType()}.
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue} 
 */
public class FunctionExplanationValue<T extends ExplanationNumberValue<T>> extends SingleValueExplanation<T> {

    private NumberFunction<T> functionHolder;

    public FunctionExplanationValue(NumberFunction<T> functionHolder) {
        this.functionHolder = functionHolder;
    }

    @Override
    public String getDisplayName(int mode) {
        switch (mode) {
            case SHORT:
                return super.getDisplayName(mode);
            default:
                String f = functionHolder.getFunctionName() + '(';
                for (int i = 0; i < functionHolder.getParams().length; i++) {
                    if (i > 0) {
                        f += ", ";
                    }
                    f += functionHolder.getParams()[i].getDisplayName(mode - 1);
                }
                return super.getDisplayName(mode) + " = " + f + ')';
        }
    }

    @Override
    public String printContent(int mode, boolean fromMultiplicativeExpr, boolean inBrackets) {
        if ((mode & EXPAND_FUNCTION) == 0) {
            if (functionHolder.getResult() == null) {
                return super.printContent(mode, false, inBrackets);
            }
            return functionHolder.getResult().printContent(mode, fromMultiplicativeExpr, inBrackets);
        }
        StringBuffer buf = new StringBuffer();

        if ((mode & PRINT_VALUE_IN_EXPANDED) != 0) {
            if (!inBrackets) {
                buf.append('(');
            }
            buf.append(printValue() + "=");
        }

        buf.append(functionHolder.getFunctionName());
        buf.append('(');
        for (int i = 0; i < functionHolder.getParams().length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append(functionHolder.getParams()[i].printExplanationLocal(mode, false));
        }
        buf.append(')');
        if (!inBrackets) {
            if ((mode & PRINT_VALUE_IN_EXPANDED) != 0) {
                buf.append(')');
            }
        }

        return buf.toString();
    }

    @Override
    public Iterator<T> getChildren() {
        return OpenIterator.fromArray(functionHolder.getParams());
    }

    @Override
    public String getType() {
        return ValueType.FUNCTION.toString();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
