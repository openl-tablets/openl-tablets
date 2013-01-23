package org.openl.rules.project.instantiation.variation;

import org.openl.rules.table.InputArgumentsCloner;

/**
 * Variation that clones all arguments before the modification by another
 * variation(that is delegated.)
 * 
 * @author PUdalau, Marat Kamalov
 */
public class DeepCloningVariaion extends Variation {
    /**
     * Suffix for generated variation ID if it have not been specified.
     */
    public static final String DEEP_CLONING_SUFFIX = "[Deep Cloning]";
    
    private Variation variation;
    
    /**
     * Empty constructor required for WS data binding
     */
    public DeepCloningVariaion() {
    }
    
    /**
     * Constructs deep-cloning variation with the generated ID(ID of delegated
     * variation + {@link DeepCloningVariaion.DEEP_CLONING_SUFFIX}).
     * 
     * @param variation Delegated variation.
     */
    public DeepCloningVariaion(Variation variation) {
        this(variation.getVariationID() + DEEP_CLONING_SUFFIX, variation);
    }

    /**
     * Constructs deep-cloning variation with the specified ID.
     * 
     * @param variationID Unique variation ID.
     * @param variation Delegated variation.
     */
    public DeepCloningVariaion(String variationID, Variation variation) {
        super(variationID);
        this.variation = variation;
    }

    @Override
    public Object[] applyModification(Object[] originalArguments) {
        Object[] clonedParams = null;
        InputArgumentsCloner cloner = new InputArgumentsCloner();
        if (originalArguments != null) {
            try {
                clonedParams = cloner.deepClone(originalArguments);
            } catch (Exception ex) {
                throw new VariationRuntimeException("Original arguments deep cloning was failure.", ex);
            }
        } else {
            clonedParams = new Object[0];
        }
        return variation.applyModification(clonedParams);
    }

    @Override
    public Object currentValue(Object[] originalArguments) {
        Object[] clonedParams = null;
        InputArgumentsCloner cloner = new InputArgumentsCloner();
        if (originalArguments != null) {
            try {
                clonedParams = cloner.deepClone(originalArguments);
            } catch (Exception ex) {
                throw new VariationRuntimeException("Original arguments deep cloning was failure.", ex);
            }
        } else {
            clonedParams = new Object[0];
        }
        return variation.currentValue(clonedParams);
    }
    
    @Override
    public void revertModifications(Object[] modifiedArguments, Object previousValue) {
    }

    /**
     * @return Wrapped variation.
     */
    public Variation getDelegatedVariation() {
        return variation;
    }

}
