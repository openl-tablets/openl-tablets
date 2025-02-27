/* Copyright © 2025 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.settings.model.converter;


import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;

public class SettingValueWrapperSerializer extends JsonSerializer<Object> implements ContextualSerializer {

    private final Function<Object, Boolean> readOnlyLookup;

    public SettingValueWrapperSerializer() {
        // Default constructor must be present for Jackson
        this.readOnlyLookup = null;
    }

    private SettingValueWrapperSerializer(Function<Object, Boolean> readOnlyLookup) {
        this.readOnlyLookup = readOnlyLookup;
    }

    @Override
    public void serialize(Object value, JsonGenerator generator, SerializerProvider serializer) throws IOException {
        var isDisabled = Optional.ofNullable(readOnlyLookup)
                .map(f -> f.apply(generator.currentValue()))
                .orElse(Boolean.FALSE);
        if (isDisabled) {
            generator.writeStartObject();
            generator.writeObjectField("value", value);
            generator.writeBooleanField("readOnly", true);
            generator.writeEndObject();
        } else {
            generator.writeObject(value);
        }
    }

    @Override
    public JsonSerializer<Object> createContextual(SerializerProvider prov, BeanProperty property) {
        var annotationDef = Optional.ofNullable(property)
                .map(p -> p.getAnnotation(SettingPropertyName.class));
        if (annotationDef.isPresent()) {
            var systemPropertyName = annotationDef.get().value();
            var disabled = Props.isDisabled(systemPropertyName);
            return new SettingValueWrapperSerializer((object) -> disabled);
        }
        return this;
    }
}
