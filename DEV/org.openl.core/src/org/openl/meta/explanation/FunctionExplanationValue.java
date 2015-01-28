package org.openl.meta.explanation;

import java.util.Arrays;

import org.openl.meta.number.NumberFunction;

/**
 * Explanation implementation for functions.
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
        return "function";
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
