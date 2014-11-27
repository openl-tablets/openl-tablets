package org.openl.rules.ruleservice.databinding.jackson.org.openl.meta;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 - 2014 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.io.IOException;

import org.openl.meta.LongValue;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

public class LongValueType {
    public static class LongValueSerializer extends StdScalarSerializer<LongValue> {
        public LongValueSerializer() {
            super(LongValue.class);
        }

        @Override
        public void serialize(LongValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                                                                                               JsonGenerationException {
            jgen.writeNumber(value.getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class LongValueDeserializer extends StdScalarDeserializer<LongValue> {

        public LongValueDeserializer() {
            super(LongValue.class);
        }

        @Override
        public LongValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                                                                                JsonProcessingException {
            Long value = _parseLong(jp, ctxt);
            if (value == null) {
                return null;
            }
            return new LongValue(value);
        }
    }
}