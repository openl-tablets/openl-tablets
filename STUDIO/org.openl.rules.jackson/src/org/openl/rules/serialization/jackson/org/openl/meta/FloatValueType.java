package org.openl.rules.serialization.jackson.org.openl.meta;

/*
 * #%L
 * OpenL - Rules - Serialization
 * %%
 * Copyright (C) 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.io.IOException;

import org.openl.meta.FloatValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class FloatValueType {
    public static class FloatValueSerializer extends StdScalarSerializer<FloatValue> {
        public FloatValueSerializer() {
            super(FloatValue.class);
        }

        @Override
        public void serialize(FloatValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                               JsonGenerationException {
            jgen.writeNumber(value.getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class FloatValueDeserializer extends StdScalarDeserializer<FloatValue> {

        public FloatValueDeserializer() {
            super(FloatValue.class);
        }

        @Override
        public FloatValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                                                                                JsonProcessingException {
            Float value = _parseFloat(jp, ctxt);
            if (value == null) {
                return null;
            }
            return new FloatValue(value);
        }
    }
}