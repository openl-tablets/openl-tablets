package org.openl.message;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.util.StringUtils;

/**
 * Class defines error OpenL message abstraction. <code>OpenLErrorMessage</code> encapsulates {@link OpenLException}
 * object as source of message.
 *
 */
public class OpenLErrorMessage extends OpenLMessage {

    private final OpenLException error;

    public OpenLErrorMessage(OpenLException error) {
        super(getOpenLExceptionMessage(error), Severity.ERROR);
        this.error = Objects.requireNonNull(error);
    }

    public OpenLException getError() {
        return error;
    }

    @Override
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        printWriter.print(super.toString());
        printWriter.print("\r\n");

        if (getError() != null) {
            String url = getError().getSourceLocation();

            if (StringUtils.isNotEmpty(url)) {
                printWriter.print("    at " + url + "\r\n");
            }
            if (getError().getCause() != null && getError().getLocation() == null) {
                getError().getCause().printStackTrace(printWriter);
            }
        }

        printWriter.close();

        return stringWriter.toString();
    }

    @Override
    public String getSourceLocation() {
        return error.getSourceLocation();
    }


    private static String getOpenLExceptionMessage(OpenLException ex) {

        if (!(ex instanceof Throwable)) {
            return null;
        }

        Throwable t = (Throwable) ex;

        OpenLException cause = ex;
        while (t != null) {
            if (t instanceof OpenLException) {
                cause = (OpenLException) t;
            }
            t = t.getCause();
        }

        return cause.getMessage();
    }
}
