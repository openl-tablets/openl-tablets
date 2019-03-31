package org.openl.rules.variation;

/*
 * #%L
 * OpenL - Variation
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;

/**
 * Variation implementation using to find fields to modify JXpath (See {@link http://commons.apache.org/jxpath/}). Note:
 * path can not point on root object(path "."), only fields modifications supported. If you want to replace entire
 * argument you should use {@link ArgumentReplacementVariation}.
 * 
 * @author PUdalau, Marat Kamalov
 */

@XmlRootElement
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
     * @param variationID Unique ID of variations.
     * @param updatedArgumentIndex index of argument to modify.
     * @param path Path supported by JXPath that point to field to modify.
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
            throw new VariationRuntimeException(
                "Failed to apply variaion \"" + getVariationID() + "\". Number of argument to modify is [" + updatedArgumentIndex + "] but arguments length is " + originalArguments.length);
        }
        JXPathContext context = JXPathContext.newContext(originalArguments[updatedArgumentIndex]);
        Pointer pointer = compiledExpression.createPath(context);
        return pointer.getValue();
    }

    @Override
    public Object[] applyModification(Object[] originalArguments) {
        if (updatedArgumentIndex >= originalArguments.length) {
            throw new VariationRuntimeException(
                "Failed to apply variaion \"" + getVariationID() + "\". Number of argument to modify is [" + updatedArgumentIndex + "] but arguments length is " + originalArguments.length);
        }
        JXPathContext context = JXPathContext.newContext(originalArguments[updatedArgumentIndex]);
        Pointer pointer = compiledExpression.createPath(context);
        pointer.setValue(valueToSet);
        return originalArguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Object previousValue) {
        if (updatedArgumentIndex >= modifiedArguments.length) {
            throw new VariationRuntimeException(
                "Failed to apply variaion \"" + getVariationID() + "\". Number of argument to modify is [" + updatedArgumentIndex + "] but arguments length is " + modifiedArguments.length);
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
