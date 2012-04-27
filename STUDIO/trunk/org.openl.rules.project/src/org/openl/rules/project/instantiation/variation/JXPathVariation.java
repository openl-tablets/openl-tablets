package org.openl.rules.project.instantiation.variation;

import java.util.Stack;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.openl.exception.OpenlNotCheckedException;

/**
 * Variation implementation using to find fields to modify JXpath (See
 * {@link http://commons.apache.org/jxpath/}). Note: path can not point on root
 * object(path "."), only fields modifications supported. If you want to replace
 * entire argument you should use {@link ArgumentReplacementVariation}.
 * 
 * @author PUdalau
 */
public class JXPathVariation extends Variation {
    private int updatedArgumentIndex;
    private String path;
    private Object valueToSet;
    private CompiledExpression compiledExpression;

    /**
     * Constructs JXPath variation.
     * 
     * @param variationID Unique ID of variations.
     * @param updatedArgumentIndex index of argument to modify.
     * @param path Path supported by JXPath that point to field to modify.
     * @param valueToSet
     */
    public JXPathVariation(String variationID, int updatedArgumentIndex, String path, Object valueToSet) {
        super(variationID);
        if (updatedArgumentIndex < 0) {
            throw new IllegalArgumentException("Number of argument to be modified should be non negative.");
        } else {
            this.updatedArgumentIndex = updatedArgumentIndex;
        }
        this.path = path;
        this.valueToSet = valueToSet;
        compiledExpression = JXPathContext.compile(path);
    }

    @Override
    public Object[] applyModification(Object[] originalArguments, Stack<Object> stack) {
        if (updatedArgumentIndex >= originalArguments.length) {
            throw new OpenlNotCheckedException("Failed to apply variaion \"" + getVariationID() + "\". Number of argument to modify is [" + updatedArgumentIndex + "] but arguments length is " + originalArguments.length);
        }
        JXPathContext context = JXPathContext.newContext(originalArguments[updatedArgumentIndex]);
        Pointer pointer = compiledExpression.createPath(context);
        Object previousValue = pointer.getValue();
        stack.push(previousValue);
        pointer.setValue(valueToSet);
        return originalArguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Stack<Object> stack) {
        JXPathContext context = JXPathContext.newContext(modifiedArguments[updatedArgumentIndex]);
        Object previousValue = stack.pop();
        compiledExpression.setValue(context, previousValue);
    }

    /**
     * @return Index of arguments to be modified.
     */
    public int getUpdatedArgumentIndex() {
        return updatedArgumentIndex;
    }

    /**
     *  @return path to field to be modified.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return value to set into modified field.
     */
    public Object getValueToSet() {
        return valueToSet;
    }
}
