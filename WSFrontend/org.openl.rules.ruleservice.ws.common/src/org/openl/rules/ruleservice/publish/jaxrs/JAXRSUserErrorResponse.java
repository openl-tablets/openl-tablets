package org.openl.rules.ruleservice.publish.jaxrs;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Localized error response object
 */
@XmlRootElement
public record JAXRSUserErrorResponse(String code, String message) {
}
