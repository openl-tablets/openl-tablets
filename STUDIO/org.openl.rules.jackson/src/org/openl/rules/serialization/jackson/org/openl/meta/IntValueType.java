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

import org.openl.meta.IntValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class IntValueType {
    public static class IntValueSerializer extends StdScalarSerializer<IntValue> {
        public IntValueSerializer() {
            super(IntValue.class);
        }

        @Override
        public void serialize(IntValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                              JsonGenerationException {
            jgen.writeNumber(value.getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class IntValueDeserializer extends StdScalarDeserializer<IntValue> {

        public IntValueDeserializer() {
            super(IntValue.class);
        }

        @Override
        public IntValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                                                                               JsonProcessingException {
            Integer value = _parseInteger(jp, ctxt);
            if (value == null) {
                return null;
            }
            return new IntValue(value);
        }
    }
}