package org.openl.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class OpenLCompilationException extends Exception {

    private static final long serialVersionUID = -8075090606797764194L;

    private String message;
    private Throwable cause;
    private ILocation location;
    private IOpenSourceCodeModule source;

    public OpenLCompilationException(String message, Throwable cause, ILocation location, IOpenSourceCodeModule source) {
        this.message = message;
        this.cause = cause;
        this.location = location;
        this.source = source;
    }

    public OpenLCompilationException(String message, Throwable cause, ILocation location) {
        this(message, cause, location, null);
    }

    /**
     * Gets error message.
     * 
     * @return error message
     */
    public String getMessage() {

        Throwable originalCause = getOriginalCause();

        String errorMessage = StringUtils.EMPTY;

        if (originalCause != null) {
            errorMessage = String.format("%s [%s]", originalCause.getMessage(), originalCause.getClass().getName());
        }

        if (StringUtils.isEmpty(message)) {
            return errorMessage;
        } else if (StringUtils.isEmpty(errorMessage)) {
            return message;
        }

        return StringUtils.join(new Object[] { message, errorMessage }, "\n");
    }

    /**
     * Gets original cause of error. It can be <code>null</code> if cause is not
     * java exception or java error.
     * 
     * @return {@link Throwable} object if cause of error is java exception or
     *         java error; <code>null</code> - otherwise
     */
    public Throwable getOriginalCause() {

        if (cause == null) {
            return null;
        }

        Throwable rootCause = ExceptionUtils.getRootCause(cause);

        if (rootCause != null) {
            return rootCause;
        }

        return cause;
    }

    /**
     * Gets error cause location.
     * 
     * @return error cause location
     */
    public ILocation getLocation() {
        return location;
    }

    /**
     * Gets source code module where the error was occurred.
     * 
     * @return source code module
     */
    public IOpenSourceCodeModule getSourceModule() {
        return source;
    }

    @Override
    public String toString() {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        OpenLExceptionUtils.printError(this, printWriter);

        printWriter.close();

        return stringWriter.toString();
    }

}
