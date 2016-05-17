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
import java.math.BigInteger;

import org.openl.meta.BigIntegerValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.NumberDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class BigIntegerValueType {
    public static class BigIntegerValueSerializer extends StdScalarSerializer<BigIntegerValue> {
        public BigIntegerValueSerializer() {
            super(BigIntegerValue.class);
        }

        @Override
        public void serialize(BigIntegerValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                                     JsonGenerationException {
            jgen.writeNumber(value.getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class BigIntegerValueDeserializer extends StdScalarDeserializer<BigIntegerValue> {

        NumberDeserializer numberDeserializer = new NumberDeserializer();
        
        public BigIntegerValueDeserializer() {
            super(BigIntegerValue.class);
        }
        
        @Override
        public BigIntegerValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                                                                                      JsonProcessingException {
            Number value = (Number) numberDeserializer.deserialize(jp, ctxt);
            if (value == null) {
                return null;
            } else {
                if (value instanceof BigInteger) {
                    return new BigIntegerValue((BigInteger) value);
                }
                if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                    return new BigIntegerValue(value.toString());
                }
            }
            throw ctxt.weirdStringException(value.toString(), BigInteger.class, "not a valid number");
        }
    }
}