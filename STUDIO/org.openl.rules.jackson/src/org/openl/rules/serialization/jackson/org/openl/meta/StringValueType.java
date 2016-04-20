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

import org.openl.meta.StringValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class StringValueType {
    public static class StringValueSerializer extends StdSerializer<StringValue> {
        public StringValueSerializer() {
            super(StringValue.class);
        }

        @Override
        public void serialize(StringValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                 JsonGenerationException {
            jgen.writeString(value.getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class StringValueDeserializer extends StdDeserializer<StringValue> {

        public StringValueDeserializer() {
            super(StringValue.class);
        }

        @Override
        public StringValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                                                                                  JsonProcessingException {
            String value = _parseString(jp, ctxt);
            if (value == null) {
                return null;
            }
            return new StringValue(value);
        }
    }
}