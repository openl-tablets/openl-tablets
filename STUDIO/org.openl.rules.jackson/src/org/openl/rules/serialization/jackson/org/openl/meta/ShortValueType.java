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

import org.openl.meta.ShortValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class ShortValueType {
    public static class ShortValueSerializer extends StdScalarSerializer<ShortValue> {
        public ShortValueSerializer() {
            super(ShortValue.class);
        }

        @Override
        public void serialize(ShortValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                JsonGenerationException {
            jgen.writeNumber(value.getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class ShortValueDeserializer extends StdScalarDeserializer<ShortValue> {

        public ShortValueDeserializer() {
            super(ShortValue.class);
        }

        @Override
        public ShortValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                                                                                 JsonProcessingException {
            Short value = _parseShort(jp, ctxt);
            if (value == null) {
                return null;
            }
            return new ShortValue(value);
        }
    }
}
