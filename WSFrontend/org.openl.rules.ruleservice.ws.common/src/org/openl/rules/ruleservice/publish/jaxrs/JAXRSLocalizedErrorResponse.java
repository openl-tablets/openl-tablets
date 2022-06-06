package org.openl.rules.ruleservice.publish.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.rules.ruleservice.core.ExceptionType;

/**
 * Localized error response object
 */
@XmlRootElement
public class JAXRSLocalizedErrorResponse extends JAXRSErrorResponse {

    private final String code;

    public JAXRSLocalizedErrorResponse(String message,
            String code,
            ExceptionType type,
            String[] details) {
        super(message, type, details);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
