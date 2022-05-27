package org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.oas.models.media.XML;

public abstract class OpenApiXmlIgnoreMixIn extends SchemaMixin {
    @JsonIgnore
    public abstract XML getXml();
}
