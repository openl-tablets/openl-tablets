package org.openl.rules.variation;

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
