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

import org.openl.rules.rest.settings.model.SettingValueWrapper;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.ConfigPrefixSettingsHolder;
import org.openl.rules.webstudio.web.admin.SettingPropertyName;

public class SettingValueWrapperSerializer extends JsonSerializer<Object> implements ContextualSerializer {

    private final Function<Object, Boolean> readOnlyLookup;
    private final boolean secret;

    public SettingValueWrapperSerializer() {
        // Default constructor must be present for Jackson
        this.readOnlyLookup = null;
        secret = false;
    }

    private SettingValueWrapperSerializer(Function<Object, Boolean> readOnlyLookup, boolean secret) {
        this.readOnlyLookup = readOnlyLookup;
        this.secret = secret;
    }

    @Override
    public void serialize(Object value, JsonGenerator generator, SerializerProvider serializer) throws IOException {
        boolean isDisabled = Optional.ofNullable(readOnlyLookup)
                .map(f -> f.apply(generator.currentValue()))
                .orElse(Boolean.FALSE);
        boolean isSecret = secret && isNotEmpty(value);
        boolean wrapped = isDisabled || isSecret;
        if (wrapped) {
            var wrapper = SettingValueWrapper.builder()
                    .secret(isSecret)
                    .readOnly(isDisabled)
                    .value(value);
            generator.writeObject(wrapper.build());
        } else {
            generator.writeObject(value);
        }
    }

    private boolean isNotEmpty(Object value) {
        if (value instanceof String str) {
            return !str.isEmpty();
        }
        return value != null;
    }

    @Override
    public JsonSerializer<Object> createContextual(SerializerProvider prov, BeanProperty property) {
        var annotationDef = Optional.ofNullable(property)
                .map(p -> p.getAnnotation(SettingPropertyName.class));
        if (annotationDef.isPresent()) {
            var annotation = annotationDef.get();
            var systemPropertyName = annotation.value();
            Function<Object, Boolean> readOnlyLookup;
            if (systemPropertyName.isBlank()) {
                var systemPropertySuffix = annotation.suffix();
                readOnlyLookup = (object) -> {
                    if (object instanceof ConfigPrefixSettingsHolder holder) {
                        return Props.isDisabled(holder.getConfigPropertyKey(systemPropertySuffix));
                    }
                    return false;
                };
            } else {
                var disabled = Props.isDisabled(systemPropertyName);
                readOnlyLookup = (object) -> disabled;
            }
            return new SettingValueWrapperSerializer(readOnlyLookup, annotation.secret());
        }
        return this;
    }
}
