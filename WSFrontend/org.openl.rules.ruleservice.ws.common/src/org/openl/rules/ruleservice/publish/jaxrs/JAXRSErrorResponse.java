package org.openl.rules.ruleservice.publish.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.rules.ruleservice.core.ExceptionType;

@XmlRootElement
public class JAXRSErrorResponse {
    private final String message;
    private final ExceptionType type;

    public JAXRSErrorResponse(String message, ExceptionType type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public ExceptionType getType() {
        return type;
    }

}
