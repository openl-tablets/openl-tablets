package org.openl.rules.project.instantiation.variation;

import java.util.Stack;

/**
 * Empty variation that represents original calculation without any changes of
 * arguments.
 * 
 * @author PUdalau
 */
public class NoVariation extends Variation {
    /**
     * ID for original calculation. Can be used for retrieving result of
     * calculation withou variations.
     */
    public static final String ORIGIANAL_CALCULATION = "Original calcuation";

    public NoVariation() {
        super(ORIGIANAL_CALCULATION);
    }

    @Override
    public Object[] applyModification(Object[] originalArguments, Stack<Object> stack) {
        return originalArguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Stack<Object> stack) {
    }

}
