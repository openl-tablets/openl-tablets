package org.openl.exception;

import java.io.Serial;

import lombok.Getter;

import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * Parent for OpenL Java runtime exceptions.
 */
public class OpenlNotCheckedException extends RuntimeException implements OpenLException {

    @Serial
    private static final long serialVersionUID = -4044064134031015107L;

    @Getter
    private final transient ILocation location;
    @Getter
    private final String sourceCode;
    @Getter
    private final String sourceLocation;

    public OpenlNotCheckedException() {
        this.location = null;
        this.sourceCode = null;
        this.sourceLocation = null;
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

    public OpenlNotCheckedException(String message,
                                    Throwable cause,
                                    ILocation location,
                                    IOpenSourceCodeModule sourceModule) {
        super(message, cause);
        this.location = location;
        this.sourceCode = sourceModule != null ? sourceModule.getCode() : null;
        this.sourceLocation = SourceCodeURLTool.makeSourceLocationURL(location, sourceModule);
    }
}
