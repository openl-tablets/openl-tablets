package org.openl.rules.project.instantiation;

/**
 * Appears during the compilation or instantiation of rules.
 *
 * @author PUdalau
 */
public class RulesInstantiationException extends Exception {
    private static final long serialVersionUID = -9185342823934771219L;

    public RulesInstantiationException(String message) {
        super(message);
    }

    public RulesInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RulesInstantiationException(Throwable cause) {
        super(cause);
    }
}
