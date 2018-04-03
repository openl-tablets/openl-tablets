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
 * Simple bean that defines single variation. Used in case when variation are
 * defined in rules(See {@link VariationsFromRules}).
 * 
 * @author PUdalau
 */
@Deprecated
public class VariationDescription {
    private String variationID;
    private int updatedArgumentIndex;
    private String path;
    private Object valueToSet;
    private boolean useCloning;

    /**
     * Constructs empty draft of variation for future filling up all parameters:
     * variationID, path to field, argument index, value to set.
     */
    public VariationDescription() {
        this(null, 0, VariationsFactory.THIS_POINTER, null);
    }

    /**
     * Construct non-cloning variation description using specified parameters.
     * 
     * @param variationID The unique identifier for current variation.
     * @param updatedArgumentIndex Index of argument to modify.
     * @param path Path to modified field.
     * @param valueToSet Value to set by path.
     */
    public VariationDescription(String variationID, int updatedArgumentIndex, String path, Object valueToSet) {
        this(variationID, updatedArgumentIndex, path, valueToSet, false);
    }

    /**
     * Construct variation description using specified parameters.
     * 
     * @param variationID The unique identifier for current variation.
     * @param updatedArgumentIndex Index of argument to modify.
     * @param path Path to modified field.
     * @param valueToSet Value to set by path.
     * @param useCloning Flag that represents whether deep cloning should be
     *            used or just modifications in arguments.
     */
    public VariationDescription(String variationID,
            int updatedArgumentIndex,
            String path,
            Object valueToSet,
            boolean useCloning) {
        this.variationID = variationID;
        this.updatedArgumentIndex = updatedArgumentIndex;
        this.path = path;
        this.valueToSet = valueToSet;
        this.useCloning = useCloning;
    }

    /**
     * @return The unique identifier for current variation.
     */
    public String getVariationID() {
        return variationID;
    }

    public void setVariationID(String variationID) {
        this.variationID = variationID;
    }

    /**
     * @return zero-based index of argument to be modified.
     */
    public int getUpdatedArgumentIndex() {
        return updatedArgumentIndex;
    }

    public void setUpdatedArgumentIndex(int updatedArgumentIndex) {
        this.updatedArgumentIndex = updatedArgumentIndex;
    }

    /**
     * @return Path to field to be modified or
     *         {@link VariationsFactory.THIS_POINTER} if entire root
     *         object(argument) should be changed.
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return Value to set by path.
     */
    public Object getValueToSet() {
        return valueToSet;
    }

    public void setValueToSet(Object valueToSet) {
        this.valueToSet = valueToSet;
    }

    /**
     * @return Flag that represents whether deep cloning should be used or just
     *         modifications in arguments.
     */
    public boolean isUseCloning() {
        return useCloning;
    }

    public void setUseCloning(boolean useCloning) {
        this.useCloning = useCloning;
    }

}
