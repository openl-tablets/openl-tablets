package org.openl.rules.serialization;

/*
 * #%L
 * OpenL - Rules - Serialization
 * %%
 * Copyright (C) 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.openl.rules.ruleservice.databinding.annotation.BindingConfigurationUtils;
import org.openl.rules.ruleservice.databinding.annotation.MixInClass;
import org.openl.rules.ruleservice.databinding.annotation.MixInRulesClass;
import org.openl.rules.serialization.jackson.Mixin;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.ArgumentReplacementVariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.ComplexVariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.DeepCloningVariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.JXPathVariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.VariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.VariationsResultType;
import org.openl.rules.variation.ArgumentReplacementVariation;
import org.openl.rules.variation.ComplexVariation;
import org.openl.rules.variation.DeepCloningVariation;
import org.openl.rules.variation.JXPathVariation;
import org.openl.rules.variation.Variation;
import org.openl.rules.variation.VariationsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.Builder;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class JacksonObjectMapperFactoryBean {

    private final Logger log = LoggerFactory.getLogger(JacksonObjectMapperFactoryBean.class);

    private boolean supportVariations = false;

    private DefaultTypingMode defaultTypingMode = DefaultTypingMode.SMART;

    private DateFormat defaultDateFormat = getISO8601Format();

    private JsonInclude.Include serializationInclusion;

    private Set<String> overrideTypes;

    private boolean failOnUnknownProperties = false;

    private boolean polymorphicTypeValidation = false;

    public ObjectMapper createJacksonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

        AnnotationIntrospector primaryIntrospector = new JacksonAnnotationIntrospector();
        JaxbAnnotationIntrospector secondaryIntropsector = new JaxbAnnotationIntrospector(
            TypeFactory.defaultInstance());

        if (serializationInclusion != null) {
            mapper.setSerializationInclusion(serializationInclusion);
            secondaryIntropsector.setNonNillableInclusion(serializationInclusion);
        }

        AnnotationIntrospector introspector = new AnnotationIntrospectorPair(primaryIntrospector,
            secondaryIntropsector);

        mapper.setAnnotationIntrospector(introspector);

        if (DefaultTypingMode.SMART.equals(getDefaultTypingMode()) || DefaultTypingMode.ENABLE
            .equals(getDefaultTypingMode())) {
            Builder basicPolymorphicTypeValidatorBuilder = null;
            final boolean polymorphicTypeValidation = isPolymorphicTypeValidation();
            if (polymorphicTypeValidation) {
                basicPolymorphicTypeValidatorBuilder = BasicPolymorphicTypeValidator.builder();
                basicPolymorphicTypeValidatorBuilder.allowIfSubTypeIsArray();
            }
            if (getOverrideTypes() != null) {
                List<Class<?>> classes = new ArrayList<>();
                List<Class<?>> configurationClasses = new ArrayList<>();
                for (String className : getOverrideTypes()) {
                    try {
                        Class<?> clazz = loadClass(className);
                        if (BindingConfigurationUtils.isConfiguration(clazz)) {
                            configurationClasses.add(clazz);
                        } else {
                            classes.add(clazz);
                            if (polymorphicTypeValidation) {
                                basicPolymorphicTypeValidatorBuilder.allowIfBaseType(clazz);
                                basicPolymorphicTypeValidatorBuilder.allowIfSubType(clazz);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        log.warn("Class '{}' is not found.", className, e);
                    }
                }
                for (Class<?> clazz : configurationClasses) {
                    MixInClass mixInClass = clazz.getAnnotation(MixInClass.class);
                    if (mixInClass != null) {
                        Arrays.stream(mixInClass.value()).forEach(forClass -> mapper.addMixIn(forClass, clazz));
                    }
                    MixInRulesClass mixInRulesClass = clazz.getAnnotation(MixInRulesClass.class);
                    if (mixInRulesClass != null) {
                        for (String className : mixInRulesClass.value()) {
                            try {
                                Class<?> useForClass = loadClass(className);
                                mapper.addMixIn(useForClass, clazz);
                            } catch (ClassNotFoundException e) {
                                log.warn("Class '{}' is not found.", className, e);
                            }
                        }

                    }
                }
                for (Class<?> clazz : classes) {
                    for (Class<?> c : classes) {
                        if (!clazz.equals(c) && clazz.isAssignableFrom(c)) {
                            addMixIn(mapper, clazz, Mixin.class);
                            break;
                        }
                    }
                    mapper.registerSubtypes(clazz);
                }
            }
            mapper.activateDefaultTyping(
                polymorphicTypeValidation ? basicPolymorphicTypeValidatorBuilder.build()
                                          : LaissezFaireSubTypeValidator.instance,
                DefaultTypingMode.ENABLE.equals(getDefaultTypingMode()) ? ObjectMapper.DefaultTyping.NON_FINAL
                                                                        : ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT,
                JsonTypeInfo.As.PROPERTY);
        } else {
            mapper.deactivateDefaultTyping();
        }

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, isFailOnUnknownProperties());

        if (isSupportVariations()) {
            addMixIn(mapper, Variation.class, VariationType.class);
            addMixIn(mapper, ArgumentReplacementVariation.class, ArgumentReplacementVariationType.class);
            addMixIn(mapper, ComplexVariation.class, ComplexVariationType.class);
            addMixIn(mapper, DeepCloningVariation.class, DeepCloningVariationType.class);
            addMixIn(mapper, JXPathVariation.class, JXPathVariationType.class);
            addMixIn(mapper, VariationsResult.class, VariationsResultType.class);
        }

        if (getDefaultDateFormat() == null) {
            mapper.setDateFormat(getISO8601Format());
        } else {
            mapper.setDateFormat(getDefaultDateFormat());
        }

        return mapper;
    }

    private void addMixIn(ObjectMapper mapper, Class<?> classFor, Class<?> mixIn) {
        if (mapper.findMixInClassFor(classFor) == null) {
            mapper.addMixIn(classFor, mixIn);
        }
    }

    /**
     * Create instance of ISO-8601 date time format using following pattern: "yyyy-MM-dd'T'HH:mm:ss.SSSZ" Time zones in
     * ISO-8601 are represented as local time (with the location unspecified), as UTC, or as an offset from UTC. If no
     * UTC relation information is given with a time representation, the time is assumed to be in local time. Examples,
     * when local Time Zone is +2:
     *
     * <pre>
     *     2016-12-31T22:00:00 corresponds to 2016-12-31T22:00:00+0200 in local Time Zone
     *     2016-12-31T22:00:00Z corresponds to 2017-01-01T00:00:00+0200 in local Time Zone
     *     2016-12-31T22:00:00+0200 corresponds to 2016-12-31T22:00:00+0200 in local Time Zone
     *     2016-12-31T22:00:00+0300 corresponds to 2016-12-31T21:00:00+0200 in local Time Zone
     * </pre>
     *
     * @see <a href= "https://en.wikipedia.org/wiki/ISO_8601#Time_zone_designators">ISO-8601 Time zone designators</a>
     * @return
     */
    private static DateFormat getISO8601Format() {
        StdDateFormat iso8601Format = new StdDateFormat();
        iso8601Format.setTimeZone(TimeZone.getDefault());
        return iso8601Format;
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

    public DefaultTypingMode getDefaultTypingMode() {
        return defaultTypingMode;
    }

    public void setDefaultTypingMode(DefaultTypingMode defaultTypingMode) {
        if (defaultTypingMode == null) {
            this.defaultTypingMode = DefaultTypingMode.SMART;
        } else {
            this.defaultTypingMode = defaultTypingMode;
        }
    }

    public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {
        this.failOnUnknownProperties = failOnUnknownProperties;
    }

    public boolean isFailOnUnknownProperties() {
        return failOnUnknownProperties;
    }

    public Set<String> getOverrideTypes() {
        return overrideTypes;
    }

    public void setOverrideTypes(Set<String> overrideTypes) {
        this.overrideTypes = overrideTypes;
    }

    public DateFormat getDefaultDateFormat() {
        return defaultDateFormat;
    }

    public void setDefaultDateFormat(DateFormat defaultDateFormat) {
        this.defaultDateFormat = defaultDateFormat;
    }

    public JsonInclude.Include getSerializationInclusion() {
        return serializationInclusion;
    }

    public void setSerializationInclusion(JsonInclude.Include serializationInclusion) {
        this.serializationInclusion = serializationInclusion;
    }

    public boolean isPolymorphicTypeValidation() {
        return polymorphicTypeValidation;
    }

    public void setPolymorphicTypeValidation(boolean polymorphicTypeValidation) {
        this.polymorphicTypeValidation = polymorphicTypeValidation;
    }
}
