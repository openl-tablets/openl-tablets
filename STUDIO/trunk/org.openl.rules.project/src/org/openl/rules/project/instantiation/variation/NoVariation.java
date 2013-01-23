    package org.openl.rules.project.instantiation.variation;

/**
 * Empty variation that represents original calculation without any changes of
 * arguments.
 * 
 * @author PUdalau, Marat Kamalov
 */
public class NoVariation extends Variation {
    /**
     * ID for original calculation. Can be used for retrieving result of
     * calculation without variations.
     */
    public static final String ORIGIANAL_CALCULATION = "Original calcuation";

    public NoVariation() {
        super(ORIGIANAL_CALCULATION);
    }

    @Override
    public Object[] applyModification(Object[] originalArguments) {
        return originalArguments;
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Object previousValue) {
    }

    @Override
    public Object currentValue(Object[] originalArguments) {
        return null;
    }
}
