package org.openl.rules.table.xls;

/**
 * Exception to handle evaluating incorrect formulas in POI. When we try to evaluate it,
 * POI throws RuntimeException, this is a class to wrap this kind of exceptions.
 * @author DLiauchuk
 *
 */
public class IncorrectFormulaException extends RuntimeException {

    private static final long serialVersionUID = 123L;

    public IncorrectFormulaException() {
        super();
    }

    public IncorrectFormulaException(String msg) {
        super(msg);
    }

    public IncorrectFormulaException(Throwable cause) {
        super(cause);
    }

    public IncorrectFormulaException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
