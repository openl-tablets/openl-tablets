package org.openl.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class OpenLCompilationException extends Exception implements OpenLException {

    private static final long serialVersionUID = -8075090606797764194L;

    private Throwable cause;
    private ILocation location;
    private IOpenSourceCodeModule source;

    public OpenLCompilationException(String message, Throwable cause, ILocation location, IOpenSourceCodeModule source) {
        super(message);
        this.cause = cause;
        this.location = location;
        this.source = source;
    }

    public OpenLCompilationException(String message, Throwable cause, ILocation location) {
        this(message, cause, location, null);
    }
    
    public OpenLCompilationException(String message) {
        this(message, null, null, null);
    }

    /* (non-Javadoc)
     * @see org.openl.exception.OpenLException#getOriginalMessage()
     */
    public String getOriginalMessage() {

        Throwable originalCause = getOriginalCause();

        if (originalCause != null) {
            
            String message = originalCause.getMessage();
            
            if (StringUtils.isNotBlank(message)) {
                return message;
            }
        }

        return getMessage();
    }

    /* (non-Javadoc)
     * @see org.openl.exception.OpenLException#getOriginalCause()
     */
    public Throwable getOriginalCause() {

        Throwable rootCause = ExceptionUtils.getRootCause(cause);

        if (rootCause != null) {
            return rootCause;
        }

        return cause;
    }

    /* (non-Javadoc)
     * @see org.openl.exception.OpenLException#getLocation()
     */
    public ILocation getLocation() {
        return location;
    }

    /* (non-Javadoc)
     * @see org.openl.exception.OpenLException#getSourceModule()
     */
    public IOpenSourceCodeModule getSourceModule() {
        return source;
    }

    @Override
    public String toString() {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        OpenLExceptionUtils.printError(this, printWriter);
        SourceCodeURLTool.printSourceLocation(this, printWriter);
        printWriter.close();

        return stringWriter.toString();
    }
}
