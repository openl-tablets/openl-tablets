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
 * Empty variation that represents original calculation without any changes of arguments.
 *
 * @author PUdalau, Marat Kamalov
 */

@XmlRootElement
public class NoVariation extends Variation {
    /**
     * ID for original calculation. Can be used for retrieving result of calculation without variations.
     */
    public static final String ORIGINAL_CALCULATION = "Original calculation";

    public NoVariation() {
        super(ORIGINAL_CALCULATION);
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
