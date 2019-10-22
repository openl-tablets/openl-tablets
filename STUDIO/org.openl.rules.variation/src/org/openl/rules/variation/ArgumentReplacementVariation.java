package org.openl.rules.variation;

import javax.xml.bind.annotation.XmlRootElement;

/*
 * #%L
 * OpenL - Variation
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

/**
 * Variation for replacement of value of some argument.
 *
 * It was introduced because field modification variations cannot modify root object(argument of method).
 *
 * @author PUdalau
 */
@XmlRootElement
public class ArgumentReplacementVariation extends Variation {
    private int updatedArgumentIndex;
    private Object valueToSet;

    /**
     * Constructs variation
     */
    public ArgumentReplacementVariation() {
    }

    /**
     * Constructs variation.
     *
     * @param variationID Unique ID for variation.
     * @param updatedArgumentIndex Index of argument to be updated.
     * @param valueToSet Value that will be set to instead of argument.
     */
    public ArgumentReplacementVariation(String variationID, int updatedArgumentIndex, Object valueToSet) {
        super(variationID);
        if (updatedArgumentIndex < 0) {
            throw new IllegalArgumentException("Number of argument to be modified must be non negative.");
        } else {
            this.updatedArgumentIndex = updatedArgumentIndex;
        }
        this.valueToSet = valueToSet;
    }

    @Override
    public Object currentValue(Object[] originalArguments) {
        return originalArguments[updatedArgumentIndex];
    }

    @Override
    public Object[] applyModification(Object[] originalArguments) {
        if (updatedArgumentIndex >= originalArguments.length) {
            throw new VariationRuntimeException(String.format(
                "Failed to apply variaion '%s'. Index of argument to modify is [%s] but arguments array length is %s.",
                getVariationID(),
                updatedArgumentIndex,
                originalArguments.length));
        }
        originalArguments[updatedArgumentIndex] = valueToSet;
        return originalArguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Object previousValue) {
        modifiedArguments[updatedArgumentIndex] = previousValue;
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
     * @return value to set into field.
     */
    public Object getValueToSet() {
        return valueToSet;
    }

    public void setValueToSet(Object valueToSet) {
        this.valueToSet = valueToSet;
    }
}
