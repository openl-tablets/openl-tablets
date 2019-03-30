package org.openl.exception;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * Parent for OpenL Java runtime exceptions.
 * 
 */
public class OpenlNotCheckedException extends RuntimeException implements OpenLException {

    private static final long serialVersionUID = -4044064134031015107L;
    
    private ILocation location;
    private IOpenSourceCodeModule sourceModule;
    
    public OpenlNotCheckedException() {        
    }
    
    public OpenlNotCheckedException(String message) {
        this(message, null);
    }
    
    public OpenlNotCheckedException(Throwable cause) {
        this(null, cause);
    }
    
    public OpenlNotCheckedException(String message, Throwable cause) {
        this(message, cause, null, null);
    }
    
    public OpenlNotCheckedException(String message, Throwable cause, ILocation location, IOpenSourceCodeModule sourceModule) {
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
