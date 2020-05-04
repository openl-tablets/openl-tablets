package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.models.media.XML;

public abstract class OpenApiXmlIgnoreMixIn {
    @JsonIgnore
    public abstract XML getXml();
}
