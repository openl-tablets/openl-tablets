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
 * Complex variation combines multiple variations that all will be applied to arguments sequentially.
 *
 * @author PUdalau
 */
@XmlRootElement
public class ComplexVariation extends Variation {
    private Variation[] variations;

    public ComplexVariation() {
    }

    /**
     * Constructs complex variation with the specified ID.
     *
     * @param variationID Unique ID for this variation.
     * @param variations Variations that composes this complex variation.
     */
    public ComplexVariation(String variationID, Variation... variations) {
        super(variationID);
        this.variations = variations;
    }

    /**
     * Constructs complex variation with generated ID.
     *
     * @param variationID Unique ID for this variation.
     * @param variations Variations that composes this complex variation.
     */
    public ComplexVariation(Variation... variations) {
        this(createVariationID(variations), variations);
    }

    /**
     * Generates ID for complex variation.
     *
     * @param variations Variations.
     * @return ID for complex variation
     */
    public static String createVariationID(Variation[] variations) {
        StringBuilder builder = new StringBuilder();
        builder.append("Complex variation [").append(variations.length).append("]{");
        for (int i = 0; i < variations.length; i++) {
            builder.append(variations[i].getVariationID());
            if (i != variations.length - 1) {
                builder.append(", ");
            }
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public Object currentValue(Object[] originalArguments) {
        Object[] values = new Object[variations.length];
        for (int i = 0; i < variations.length; i++) {
            values[i] = variations[i].applyModification(originalArguments);
        }
        return values;
    }

    @Override
    public Object[] applyModification(Object[] originalArguments) {
        Object[] arguments = originalArguments;
        for (int i = 0; i < variations.length; i++) {
            arguments = variations[i].applyModification(arguments);
        }
        return arguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Object previousValue) {
        Object[] values;
        if (previousValue instanceof Object[]) {
            values = (Object[]) previousValue;
        } else {
            throw new IllegalStateException();
        }
        for (int i = variations.length - 1; i >= 0; i--) {
            variations[i].revertModifications(modifiedArguments, values[i]);
        }
    }

    public Variation[] getVariations() {
        return variations;
    }

    public void setVariations(Variation[] variations) {
        this.variations = variations;
    }
}
