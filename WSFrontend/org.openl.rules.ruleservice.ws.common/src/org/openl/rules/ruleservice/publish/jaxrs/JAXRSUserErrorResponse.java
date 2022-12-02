package org.openl.rules.ruleservice.publish.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Localized error response object
 */
@XmlRootElement
public class JAXRSUserErrorResponse {

    private final String code;
    private final String message;

    public JAXRSUserErrorResponse(String message, String code) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
