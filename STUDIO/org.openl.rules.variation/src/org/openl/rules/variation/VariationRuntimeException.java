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
 * Variation API runtime exception
 *
 * @author Marat Kamalov
 *
 */
public class VariationRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 7155417820585354286L;

    public VariationRuntimeException() {
        super();
    }

    public VariationRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public VariationRuntimeException(String message) {
        super(message);
    }

    public VariationRuntimeException(Throwable cause) {
        super(cause);
    }

}
