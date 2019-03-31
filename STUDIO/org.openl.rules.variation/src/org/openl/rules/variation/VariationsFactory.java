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

/**
 * Factory for simple variations creation.
 * 
 * In most cases there will be created {@link JXPathVariation} instead of situations when
 * {@link VariationsFactory.THIS_POINTER} used as the path to field to be modified(in such a case
 * {@link ArgumentReplacementVariation} will be created). As an additional feature there is possibility to use deep
 * cloning in variations.
 * 
 * @author PUdalau
 */
// TODO complex variations with separator in the path + test
public abstract class VariationsFactory {
    /**
     * Path that represents modification of root object(argument).
     */
    public static final String THIS_POINTER = ".";

    /**
     * Creates variation using all needed parameters.
     * 
     * @param vairationId Id for variation.
     * @param argumentIndex Index of argument to be modified.
     * @param path Path to field that will be modified. Or {@link VariationsFactory.THIS_POINTER} for argument
     *            replacemnt variation.
     * @param valueToSet Value to set for modified field.
     * @return Variation that corresponds the specified path.
     */
    public static Variation getVariation(String vairationId, int argumentIndex, String path, Object valueToSet) {
        return getVariation(vairationId, argumentIndex, path, valueToSet, false);
    }

    /**
     * Creates variation using all needed parameters with possibility to use cloning for variations.
     * 
     * @param vairationId Id for variation.
     * @param argumentIndex Index of argument to be modified.
     * @param path Path to field that will be modified. Or {@link VariationsFactory.THIS_POINTER} for argument
     *            replacemnt variation.
     * @param valueToSet Value to set for modified field.
     * @param cloneArguments Flag that determines whether the created variation should be wrapped by
     *            {@link DeepCloningVariation}.
     * @return Variation that corresponds the specified path.
     */
    public static Variation getVariation(String vairationId,
            int argumentIndex,
            String path,
            Object valueToSet,
            boolean cloneArguments) {
        // TODO check null ID
        Variation variation = null;
        if (path.equals(THIS_POINTER)) {
            variation = new ArgumentReplacementVariation(vairationId, argumentIndex, valueToSet);
        } else {
            variation = new JXPathVariation(vairationId, argumentIndex, path, valueToSet);
        }
        if (cloneArguments) {
            variation = new DeepCloningVariation(variation);
        }
        return variation;
    }
}
