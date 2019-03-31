package org.openl.rules.ruleservice.publish.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JAXRSErrorResponse {
    private String message;
    private String type;
    private String[] details;

    public JAXRSErrorResponse(String message, String type, String[] details) {
        this.message = message;
        this.type = type;
        this.details = details;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String[] getDetails() {
        return details;
    }

}
