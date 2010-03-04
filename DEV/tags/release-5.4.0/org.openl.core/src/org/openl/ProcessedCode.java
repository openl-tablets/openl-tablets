package org.openl;

import org.openl.binding.IBoundCode;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;

/**
 * Class that used as a container and provides information about processed code.
 */
public class ProcessedCode {

    private static final ISyntaxError[] EMPTY_ERRORS_ARRAY = new ISyntaxError[0];

    /**
     * {@link IParsedCode} instance.
     */
    private IParsedCode parsedCode;

    /**
     * {@link IBoundCode} instance.
     */
    private IBoundCode boundCode;

    /**
     * Gets parsed code.
     * 
     * @return {@link IParsedCode} instance
     */
    public IParsedCode getParsedCode() {
        return parsedCode;
    }

    /**
     * Sets parsed code.
     * 
     * @param parsedCode {@link IParsedCode} instance
     */
    public void setParsedCode(IParsedCode parsedCode) {
        this.parsedCode = parsedCode;
    }

    /**
     * Gets bound code.
     * 
     * @return {@link IBoundCode} instance
     */
    public IBoundCode getBoundCode() {
        return boundCode;
    }

    /**
     * Sets bound code.
     * 
     * @return {@link IBoundCode} instance
     */
    public void setBoundCode(IBoundCode boundCode) {
        this.boundCode = boundCode;
    }

    /**
     * Gets errors what was found during parsing operation.
     * 
     * @return {@link ISyntaxError}s array
     */
    public ISyntaxError[] getParsingErrors() {

        if (parsedCode == null) {
            return EMPTY_ERRORS_ARRAY;
        }

        return parsedCode.getErrors();
    }

    /**
     * Gets errors what was found during binding operation.
     * 
     * @return {@link ISyntaxError}s array
     */
    public ISyntaxError[] getBindingErrors() {

        if (boundCode == null) {
            return EMPTY_ERRORS_ARRAY;
        }

        return boundCode.getErrors();
    }
}
