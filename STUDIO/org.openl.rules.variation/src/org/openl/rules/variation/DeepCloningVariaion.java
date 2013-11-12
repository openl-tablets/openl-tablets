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


import com.rits.cloning.Cloner;

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
    
    private static final Cloner cloner = ArgumentsClonerFactory.getCloner();
    
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
