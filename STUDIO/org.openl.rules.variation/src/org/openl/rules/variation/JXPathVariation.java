package org.openl.rules.variation;

import  jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;

/**
 * Variation implementation using to find fields to modify JXpath (See {@link http://commons.apache.org/jxpath/}). Note:
 * path cannot point on root object(path "."), only fields modifications supported. If you want to replace entire
 * argument you should use {@link ArgumentReplacementVariation}.
 *
 * @author PUdalau, Marat Kamalov
 */

@XmlRootElement
@Deprecated
public class JXPathVariation extends Variation {
    private int updatedArgumentIndex;
    private String path;
    private Object valueToSet;
    private CompiledExpression compiledExpression;

    /**
     * Constructs JXPath variation.
     */
    public JXPathVariation() {
    }

    /**
     * Constructs JXPath variation.
     *
     * @param variationID          Unique ID of variations.
     * @param updatedArgumentIndex index of argument to modify.
     * @param path                 Path supported by JXPath that point to field to modify.
     * @param valueToSet
     */
    public JXPathVariation(String variationID, int updatedArgumentIndex, String path, Object valueToSet) {
        super(variationID);
        if (updatedArgumentIndex < 0) {
            throw new IllegalArgumentException("Number of arguments to be modified must be non negative.");
        } else {
            this.updatedArgumentIndex = updatedArgumentIndex;
        }
        this.path = path;
        this.valueToSet = valueToSet;
        this.compiledExpression = JXPathContext.compile(path);
    }

    @Override
    public Object currentValue(Object[] originalArguments) {
        if (updatedArgumentIndex >= originalArguments.length) {
            throw new VariationRuntimeException(String.format(
                    "Failed to apply variation '%s'. Index of argument to modify is [%s] but arguments array length is %s.",
                    getVariationID(),
                    updatedArgumentIndex,
                    originalArguments.length));
        }
        JXPathContext context = JXPathContext.newContext(originalArguments[updatedArgumentIndex]);
        Pointer pointer = compiledExpression.createPath(context);
        return pointer.getValue();
    }

    @Override
    public Object[] applyModification(Object[] originalArguments) {
        if (updatedArgumentIndex >= originalArguments.length) {
            throw new VariationRuntimeException(String.format(
                    "Failed to apply variation '%s'. Index of argument to modify is [%s] but arguments array length is %s.",
                    getVariationID(),
                    updatedArgumentIndex,
                    originalArguments.length));
        }
        JXPathContext context = JXPathContext.newContext(originalArguments[updatedArgumentIndex]);
        Pointer pointer = compiledExpression.createPath(context);
        pointer.setValue(valueToSet);
        return originalArguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Object previousValue) {
        if (updatedArgumentIndex >= modifiedArguments.length) {
            throw new VariationRuntimeException(String.format(
                    "Failed to apply variation '%s'. Index of argument to modify is [%s] but arguments array length is %s.",
                    getVariationID(),
                    updatedArgumentIndex,
                    modifiedArguments.length));
        }
        JXPathContext context = JXPathContext.newContext(modifiedArguments[updatedArgumentIndex]);
        compiledExpression.setValue(context, previousValue);
    }

    /**
     * @return Index of arguments to be modified.
     */
    public int getUpdatedArgumentIndex() {
        return updatedArgumentIndex;
    }

    public void setUpdatedArgumentIndex(int updatedArgumentIndex) {
        this.updatedArgumentIndex = updatedArgumentIndex;
    }

    /**
     * @return path to field to be modified.
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return value to set into modified field.
     */
    public Object getValueToSet() {
        return valueToSet;
    }

    public void setValueToSet(Object valueToSet) {
        this.valueToSet = valueToSet;
    }
}
