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

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import org.openl.meta.DoubleValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class DoubleValueType {
    public static class DoubleValueSerializer extends StdScalarSerializer<DoubleValue> {
        public DoubleValueSerializer() {
            super(DoubleValue.class);
        }

        @Override
        public void serialize(DoubleValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                 JsonGenerationException {
            jgen.writeNumber(value.getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class DoubleValueDeserializer extends StdScalarDeserializer<DoubleValue> {

        private static final NumberDeserializers.DoubleDeserializer DESERIALIZER = new NumberDeserializers.DoubleDeserializer(Double.class, null);

        public DoubleValueDeserializer() {
            super(DoubleValue.class);
        }

        @Override
        public DoubleValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                                                                                  JsonProcessingException {
            Double value = DESERIALIZER.deserialize(jp, ctxt);
            if (value == null) {
                return null;
            }
            return new DoubleValue(value);
        }
    }
}