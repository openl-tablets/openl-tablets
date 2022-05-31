package org.openl.rules.ruleservice.publish.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.rules.ruleservice.core.ExceptionType;

/**
 * Localized error response object
 */
@XmlRootElement
public class JAXRSLocalizedErrorResponse extends JAXRSErrorResponse {

    private final String code;
    private final Object[] args;

    public JAXRSLocalizedErrorResponse(String message,
            String code,
            Object[] args,
            ExceptionType type,
            String[] details) {
        super(message, type, details);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
