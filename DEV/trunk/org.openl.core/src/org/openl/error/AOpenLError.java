package org.openl.error;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public abstract class AOpenLError extends Exception implements IOpenLError {

    private static final long serialVersionUID = -8075090606797764194L;

    private String message;
    private Throwable cause;
    private ILocation location;
    private IOpenSourceCodeModule source;

    public AOpenLError(String message, Throwable cause, ILocation location, IOpenSourceCodeModule source) {
        this.message = message;
        this.cause = cause;
        this.location = location;
        this.source = source;
    }

    public AOpenLError(String message, Throwable cause, ILocation location) {
        this(message, cause, location, null);
    }

    public ILocation getLocation() {
        return location;
    }

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

    public IOpenSourceCodeModule getSourceModule() {
        return source;
    }

    @Override
    public String toString() {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        OpenLErrorUtils.printError(this, printWriter);

        printWriter.close();

        return stringWriter.toString();
    }

}
