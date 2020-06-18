package org.openl.rules.ruleservice.publish.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.rules.ruleservice.core.ExceptionType;

@XmlRootElement
public class JAXRSErrorResponse {
    private final String message;
    private final ExceptionType type;
    private final String[] details;

    public JAXRSErrorResponse(String message, ExceptionType type, String[] details) {
        this.message = message;
        this.type = type;
        this.details = details;
    }

    public String getMessage() {
        return message;
    }

    public ExceptionType getType() {
        return type;
    }

    public String[] getDetails() {
        return details;
    }

}
