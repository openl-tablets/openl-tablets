package org.openl.rules.activiti.spring.result;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class ResultValueConvertException extends OpenlNotCheckedException {

    private static final long serialVersionUID = -5594867331268598823L;

    public ResultValueConvertException() {
        super();
    }

    public ResultValueConvertException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

    public ResultValueConvertException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultValueConvertException(String message) {
        super(message);
    }

    public ResultValueConvertException(Throwable cause) {
        super(cause);
    }

}
