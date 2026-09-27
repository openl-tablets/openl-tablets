package org.openl.exception;

import java.io.Serial;

import lombok.Getter;

import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

/**
 * Parent for OpenL Java exceptions.
 */
public class OpenLCheckedException extends Exception implements OpenLException {

    @Serial
    private static final long serialVersionUID = -4044064134031015107L;

    @Getter
    private final transient ILocation location;
    @Getter
    private final String sourceCode;
    @Getter
    private final String sourceLocation;

    public OpenLCheckedException() {
        this.location = null;
        this.sourceCode = null;
        this.sourceLocation = null;
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
        this.sourceCode = sourceModule != null ? sourceModule.getCode() : null;
        this.sourceLocation = SourceCodeURLTool.makeSourceLocationURL(location, sourceModule);
    }
}
