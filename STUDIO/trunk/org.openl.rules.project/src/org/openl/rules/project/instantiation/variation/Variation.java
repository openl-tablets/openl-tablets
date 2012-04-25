package org.openl.rules.project.instantiation.variation;

import java.util.Stack;

/**
 * Common variations class. It should have unique ID and handle two phases:
 * modifying arguments before the calculations and rolling back changes after
 * execution.
 * 
 * To store previous values of changed fields there can be used stack passed as
 * argument(if it is needed.)
 * 
 * @author PUdalau
 */
public abstract class Variation {
    private String variationID;

    /**
     * Constructs variation with the ID.
     * 
     * @param variationID Unique ID.
     */
    public Variation(String variationID) {
        this.variationID = variationID;
    }

    /**
     * @return Unique ID of this variation.
     */
    public String getVariationID() {
        return variationID;
    }

    /**
     * Modifies original arguments before the calculation.
     * 
     * @param originalArguments Original arguments for calculation.
     * @param stack The Stack instance to store previous values of changed
     *            fields.
     * @return Modified arguments.
     */
    public abstract Object[] applyModification(Object[] originalArguments, Stack<Object> stack);

    /**
     * Reverts changes of arguments after the calculation.
     * 
     * @param modifiedArguments Modified arguments.
     * @param stack Stack where previous values of modified fields were stored.
     */
    public abstract void revertModifications(Object[] modifiedArguments, Stack<Object> stack);

}
