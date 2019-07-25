package org.openl.exception;

import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * Parent for OpenL Java exceptions.
 *
 */
public class OpenLCheckedException extends Exception implements OpenLException {

    private static final long serialVersionUID = -4044064134031015107L;

    private ILocation location;
    private String sourceCode;
    private String sourceLocation;

    public OpenLCheckedException() {
    }

    public OpenLCheckedException(String message) {
        this(message, null);
    }

    public OpenLCheckedException(Throwable cause) {
        this(null, cause);
    }

    public OpenLCheckedException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    public OpenLCheckedException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause);
        this.location = location;
        if (sourceModule != null) {
            this.sourceCode = sourceModule.getCode();
        }
        this.sourceLocation = SourceCodeURLTool.makeSourceLocationURL(location, sourceModule);
    }

    @Override
    public ILocation getLocation() {
        return location;
    }

    @Override
    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public String getSourceLocation() {
        return sourceLocation;
    }
}
