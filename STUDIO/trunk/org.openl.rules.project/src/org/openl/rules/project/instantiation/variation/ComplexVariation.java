package org.openl.rules.project.instantiation.variation;

import java.util.Stack;

/**
 * Complex variation combines multiple variations that all will be applied to
 * arguments sequentially.
 * 
 * @author PUdalau
 */
public class ComplexVariation extends Variation {
    private Variation[] variations;

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
        builder.append("Complex variation [" + variations.length + "]{");
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
    public Object[] applyModification(Object[] originalArguments, Stack<Object> stack) {
        Object[] arguments = originalArguments;
        for (int i = 0; i < variations.length; i++) {
            arguments = variations[i].applyModification(arguments, stack);
        }
        return arguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Stack<Object> stack) {
        for (int i = variations.length - 1; i >= 0; i--) {
            variations[i].applyModification(modifiedArguments, stack);
        }
    }

    public Variation[] getVariations() {
        return variations;
    }
}
