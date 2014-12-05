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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.meta.StringValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.ruleservice.databinding.jackson.Mixin;
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
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.calc.SpreadSheetResultType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.context.IRulesRuntimeContextType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.helpers.DoubleRangeType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.helpers.IntRangeType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.table.PointType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.variation.ArgumentReplacementVariationType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.variation.ComplexVariationType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.variation.DeepCloningVariationType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.variation.JXPathVariationType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.variation.VariationType;
import org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.variation.VariationsResultType;
import org.openl.rules.table.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JacksonObjectMapperFactoryBean {

    private final Logger log = LoggerFactory.getLogger(JacksonObjectMapperFactoryBean.class);

    private boolean supportVariations = false;

    private boolean enableDefaultTyping = false;

    private Set<String> overrideTypes;

    public ObjectMapper createJacksonDatabinding() {
        ObjectMapper mapper = new ObjectMapper();

        if (isEnableDefaultTyping()) {
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        } else {
            if (getOverrideTypes() != null) {
                List<Class<?>> clazzes = new ArrayList<Class<?>>();
                for (String className : getOverrideTypes()) {
                    try {
                        Class<?> clazz = loadClass(className);
                        clazzes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        log.warn("Class \"{}\" not found!", className, e);
                    }
                }
                Iterator<Class<?>> itr = clazzes.iterator();
                while (itr.hasNext()) {
                    Class<?> clazz = itr.next();
                    Iterator<Class<?>> innerItr = clazzes.iterator();
                    while (innerItr.hasNext()) {
                        Class<?> c = innerItr.next();
                        if (!clazz.equals(c)) {
                            if (clazz.isAssignableFrom(c)) {
                                mapper.addMixInAnnotations(clazz, Mixin.class);
                                break;
                            }
                        }
                    }
                    mapper.registerSubtypes(clazz);
                }
            }
        }

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

        mapper.registerModule(valueTypesModule);

        if (isSupportVariations()) {
            addMixInAnnotations(mapper, "org.openl.rules.variation.Variation", VariationType.class);
            addMixInAnnotations(mapper,
                "org.openl.rules.variation.ArgumentReplacementVariation",
                ArgumentReplacementVariationType.class);
            addMixInAnnotations(mapper, "org.openl.rules.variation.ComplexVariation", ComplexVariationType.class);
            addMixInAnnotations(mapper,
                "org.openl.rules.variation.DeepCloningVariation",
                DeepCloningVariationType.class);
            addMixInAnnotations(mapper, "org.openl.rules.variation.JXPathVariation", JXPathVariationType.class);
            addMixInAnnotations(mapper, "org.openl.rules.variation.VariationsResult", VariationsResultType.class);
        }

        mapper.addMixInAnnotations(SpreadsheetResult.class, SpreadSheetResultType.class);
        mapper.addMixInAnnotations(Point.class, PointType.class);
        mapper.addMixInAnnotations(DoubleRange.class, DoubleRangeType.class);
        mapper.addMixInAnnotations(IntRange.class, IntRangeType.class);

        mapper.addMixInAnnotations(IRulesRuntimeContext.class, IRulesRuntimeContextType.class);
        mapper.addMixInAnnotations(org.openl.rules.ruleservice.context.IRulesRuntimeContext.class,
            org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.ruleservice.context.IRulesRuntimeContextType.class);

        return mapper;
    }

    private void addMixInAnnotations(ObjectMapper mapper, String className, Class<?> annotationClass) {
        try {
            mapper.addMixInAnnotations(loadClass(className), annotationClass);
        } catch (ClassNotFoundException e) {
            log.warn("Class \"{}\" not found!", className, e);
        }
    }

    private static Class<?> loadClass(String className) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    public boolean isSupportVariations() {
        return supportVariations;
    }

    public void setSupportVariations(boolean supportVariations) {
        this.supportVariations = supportVariations;
    }

    public boolean isEnableDefaultTyping() {
        return enableDefaultTyping;
    }

    public void setEnableDefaultTyping(boolean enableDefaultTyping) {
        this.enableDefaultTyping = enableDefaultTyping;
    }

    public Set<String> getOverrideTypes() {
        return overrideTypes;
    }

    public void setOverrideTypes(Set<String> overrideTypes) {
        this.overrideTypes = overrideTypes;
    }
}
