package org.openl.meta.explanation;

import java.util.Arrays;

import org.openl.meta.number.NumberOperations;

/**
 * Explanation implementation for functions.
 *
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue}
 */
public class FunctionExplanationValue<T extends ExplanationNumberValue<T>> extends SingleValueExplanation<T> {

    private NumberOperations function;
    private T[] params;

    public FunctionExplanationValue(NumberOperations function, T[] params) {
        this.function = function;
        if (params != null) {
            this.params = params.clone();
        }
    }

    @Override
    public Iterable<? extends org.openl.util.tree.ITreeElement<T>> getChildren() {
        return Arrays.asList(params);
    }

    /**
     * @return name of the function.
     */
    public String getFunctionName() {
        return function.toString();
    }

    /**
     * @return the array of function parameters.
     */
    public T[] getParams() {
        return params;
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
