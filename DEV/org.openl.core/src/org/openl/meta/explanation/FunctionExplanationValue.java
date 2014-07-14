package org.openl.meta.explanation;

import org.openl.meta.number.NumberFunction;
import org.openl.meta.number.NumberValue;
import org.openl.meta.number.NumberValue.ValueType;
import org.openl.util.OpenIterator;

import java.util.Arrays;

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
    public Iterable<? extends org.openl.util.tree.ITreeElement<T>> getChildren() {
        return Arrays.asList(functionHolder.getParams());
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
