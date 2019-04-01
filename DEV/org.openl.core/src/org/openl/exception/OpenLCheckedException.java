package org.openl.exception;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * Parent for OpenL Java exceptions.
 *
 */
public class OpenLCheckedException extends Exception implements OpenLException {

    private static final long serialVersionUID = -4044064134031015107L;

    private ILocation location;
    private IOpenSourceCodeModule sourceModule;

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
        this.sourceModule = sourceModule;
    }

    @Override
    public ILocation getLocation() {
        return location;
    }

    @Override
    public IOpenSourceCodeModule getSourceModule() {
        return sourceModule;
    }

}
