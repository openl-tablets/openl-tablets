package org.openl.rules.ruleservice.publish.jaxrs;

import jakarta.xml.bind.annotation.XmlRootElement;

import org.openl.rules.ruleservice.core.ExceptionType;

@XmlRootElement
public record JAXRSErrorResponse(String message, ExceptionType type) {
}
