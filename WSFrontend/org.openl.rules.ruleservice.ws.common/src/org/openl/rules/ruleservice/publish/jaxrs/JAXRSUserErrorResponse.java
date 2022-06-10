package org.openl.rules.ruleservice.publish.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.rules.ruleservice.core.ExceptionType;

/**
 * Localized error response object
 */
@XmlRootElement
public class JAXRSUserErrorResponse {

    private final String code;
    private final String message;
    private final ExceptionType type;

    public JAXRSUserErrorResponse(String message, String code, ExceptionType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public ExceptionType getType() {
        return type;
    }
}
