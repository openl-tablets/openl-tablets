package org.openl.rules.ruleservice.databinding;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 - 2014 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.BigDecimalValueType.BigDecimalValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.BigDecimalValueType.BigDecimalValueSerializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.BigIntegerValueType.BigIntegerValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.BigIntegerValueType.BigIntegerValueSerializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.ByteValueType.ByteValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.ByteValueType.ByteValueSerializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.DoubleValueType.DoubleValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.DoubleValueType.DoubleValueSerializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.FloatValueType.FloatValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.FloatValueType.FloatValueSerializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.IntValueType.IntValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.IntValueType.IntValueSerializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.LongValueType.LongValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.LongValueType.LongValueSerializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.ShortValueType.ShortValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.ShortValueType.ShortValueSerializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.StringValueType.StringValueDeserializer;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.meta.StringValueType.StringValueSerializer;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonObjectMapperFactoryBean {
    public ObjectMapper createAegisDatabinding() {
        ObjectMapper mapper = new ObjectMapper();
        
        mapper.enableDefaultTyping();
        
        SimpleModule valueTypesModule = new SimpleModule("OpenL Value Types", Version.unknownVersion());
        // Value Types binding configuration
        valueTypesModule.addSerializer(ByteValue.class, new ByteValueSerializer());
        valueTypesModule.addSerializer(ShortValue.class, new ShortValueSerializer());
        valueTypesModule.addSerializer(IntValue.class, new IntValueSerializer());
        valueTypesModule.addSerializer(LongValue.class, new LongValueSerializer());
        valueTypesModule.addSerializer(FloatValue.class, new FloatValueSerializer());
        valueTypesModule.addSerializer(DoubleValue.class, new DoubleValueSerializer());
        valueTypesModule.addSerializer(BigIntegerValue.class, new BigIntegerValueSerializer());
        valueTypesModule.addSerializer(BigDecimalValue.class, new BigDecimalValueSerializer());
        valueTypesModule.addSerializer(StringValue.class, new StringValueSerializer());

        valueTypesModule.addDeserializer(ByteValue.class, new ByteValueDeserializer());
        valueTypesModule.addDeserializer(ShortValue.class, new ShortValueDeserializer());
        valueTypesModule.addDeserializer(IntValue.class, new IntValueDeserializer());
        valueTypesModule.addDeserializer(LongValue.class, new LongValueDeserializer());
        valueTypesModule.addDeserializer(FloatValue.class, new FloatValueDeserializer());
        valueTypesModule.addDeserializer(DoubleValue.class, new DoubleValueDeserializer());
        valueTypesModule.addDeserializer(BigIntegerValue.class, new BigIntegerValueDeserializer());
        valueTypesModule.addDeserializer(BigDecimalValue.class, new BigDecimalValueDeserializer());
        valueTypesModule.addDeserializer(StringValue.class, new StringValueDeserializer());

        return mapper;
    }
}
