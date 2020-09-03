package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.models.Xml;

public abstract class SwaggerXmlIgnoreMixIn {
    @JsonIgnore
    public abstract Xml getXml();
}
