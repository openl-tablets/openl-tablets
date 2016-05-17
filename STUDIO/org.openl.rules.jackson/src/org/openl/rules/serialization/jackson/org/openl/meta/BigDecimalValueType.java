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

import org.openl.meta.BigDecimalValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.NumberDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class BigDecimalValueType {
    public static class BigDecimalValueSerializer extends StdScalarSerializer<BigDecimalValue> {
        public BigDecimalValueSerializer() {
            super(BigDecimalValue.class);
        }

        @Override
        public void serialize(BigDecimalValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                     JsonGenerationException {
            jgen.writeNumber(value.getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class BigDecimalValueDeserializer extends StdScalarDeserializer<BigDecimalValue> {

        NumberDeserializer numberDeserializer = new NumberDeserializer();

        public BigDecimalValueDeserializer() {
            super(BigDecimalValue.class);
        }

        @Override
        public BigDecimalValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                                                                                      JsonProcessingException {
            Number value = (Number) numberDeserializer.deserialize(jp, ctxt);
            if (value == null) {
                return null;
            } else {
                return new BigDecimalValue(value.toString());
            }
        }
    }
}
