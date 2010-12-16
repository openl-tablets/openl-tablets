package org.openl.exception;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class OpenlNotCheckedException extends RuntimeException implements OpenLException {

    private static final long serialVersionUID = -4044064134031015107L;
    
    public OpenlNotCheckedException(String message) {
        super(message);
    }
    
    public OpenlNotCheckedException(String message, Throwable e) {
        super(message, e);
    }

    public ILocation getLocation() {
        
        return null;
    }

    public IOpenSourceCodeModule getSourceModule() {
        
        return null;
    }

}
