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
 * Checked exception for variation API
 * 
 * @author Marat Kamalov
 *
 */
public class VariationException extends Exception {

    private static final long serialVersionUID = 8027370423006491250L;

    public VariationException() {
        super();
    }

    public VariationException(String message, Throwable cause) {
        super(message, cause);
    }

    public VariationException(String message) {
        super(message);
    }

    public VariationException(Throwable cause) {
        super(cause);
    }

}
