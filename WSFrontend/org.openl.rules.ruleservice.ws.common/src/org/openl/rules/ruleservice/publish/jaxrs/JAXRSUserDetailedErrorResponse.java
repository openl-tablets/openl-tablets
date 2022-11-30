package org.openl.rules.ruleservice.publish.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * User detailed error
 */
@XmlRootElement
public class JAXRSUserDetailedErrorResponse {

    @JsonUnwrapped
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private final Object errors;

    public JAXRSUserDetailedErrorResponse(Object errors) {
        this.errors = errors;
    }

    public Object getErrors() {
        return errors;
    }
}
