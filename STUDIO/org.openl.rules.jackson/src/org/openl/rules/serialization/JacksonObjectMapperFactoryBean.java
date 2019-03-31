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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.openl.rules.serialization.jackson.Mixin;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.ArgumentReplacementVariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.ComplexVariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.DeepCloningVariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.JXPathVariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.VariationType;
import org.openl.rules.serialization.jackson.org.openl.rules.variation.VariationsResultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

public class JacksonObjectMapperFactoryBean {

    private final Logger log = LoggerFactory.getLogger(JacksonObjectMapperFactoryBean.class);

    private boolean supportVariations = false;

    private DefaultTypingMode defaultTypingMode = DefaultTypingMode.SMART;

    private DateFormat defaultDateFormat = getISO8601Format();

    private Set<String> overrideTypes;

    public ObjectMapper createJacksonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        AnnotationIntrospector secondaryIntropsector = new JacksonAnnotationIntrospector();
        AnnotationIntrospector primaryIntrospector = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());

        AnnotationIntrospector introspector = new AnnotationIntrospectorPair(primaryIntrospector,
            secondaryIntropsector);

        mapper.setAnnotationIntrospector(introspector);

        if (DefaultTypingMode.ENABLE.equals(getDefaultTypingMode())) {
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        } else {
            if (DefaultTypingMode.SMART.equals(getDefaultTypingMode())) {
                mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY);
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                if (getOverrideTypes() != null) {
                    List<Class<?>> clazzes = new ArrayList<>();
                    for (String className : getOverrideTypes()) {
                        try {
                            Class<?> clazz = loadClass(className);
                            clazzes.add(clazz);
                        } catch (ClassNotFoundException e) {
                            log.warn("Class '{}' hasn't been found!", className, e);
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
            } else {
                mapper.disableDefaultTyping();
            }
        }

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

        /*
         * mapper.addMixInAnnotations(SpreadsheetResult.class,
         * SpreadSheetResultType.class); mapper.addMixInAnnotations(Point.class,
         * PointType.class); mapper.addMixInAnnotations(DoubleRange.class,
         * DoubleRangeType.class); mapper.addMixInAnnotations(IntRange.class,
         * IntRangeType.class);
         */

        /*
         * mapper.addMixInAnnotations(IRulesRuntimeContext.class,
         * IRulesRuntimeContextType.class);
         * mapper.addMixInAnnotations(org.openl.rules.ruleservice.context.
         * IRulesRuntimeContext.class,
         * org.openl.rules.ruleservice.databinding.jackson.org.openl.rules.
         * ruleservice.context.IRulesRuntimeContextType.class);
         */

        if (getDefaultDateFormat() == null) {
            mapper.setDateFormat(getISO8601Format());
        } else {
            mapper.setDateFormat(getDefaultDateFormat());
        }

        return mapper;
    }

    /**
     * Create instance of ISO-8601 date time format using following pattern:
     * "yyyy-MM-dd'T'HH:mm:ss.SSSZ" Time zones in ISO-8601 are represented as
     * local time (with the location unspecified), as UTC, or as an offset from
     * UTC. If no UTC relation information is given with a time representation,
     * the time is assumed to be in local time. Examples, when local Time Zone
     * is +2:
     * 
     * <pre>
     *     2016-12-31T22:00:00 corresponds to 2016-12-31T22:00:00+0200 in local Time Zone
     *     2016-12-31T22:00:00Z corresponds to 2017-01-01T00:00:00+0200 in local Time Zone
     *     2016-12-31T22:00:00+0200 corresponds to 2016-12-31T22:00:00+0200 in local Time Zone
     *     2016-12-31T22:00:00+0300 corresponds to 2016-12-31T21:00:00+0200 in local Time Zone
     * </pre>
     *
     * @see <a href=
     *      "https://en.wikipedia.org/wiki/ISO_8601#Time_zone_designators">ISO-8601
     *      Time zone designators</a>
     * @return
     */
    private static DateFormat getISO8601Format() {
        StdDateFormat iso8601Format = new StdDateFormat();
        iso8601Format.setTimeZone(TimeZone.getDefault());
        return iso8601Format;
    }

    private void addMixInAnnotations(ObjectMapper mapper, String className, Class<?> annotationClass) {
        try {
            mapper.addMixInAnnotations(loadClass(className), annotationClass);
        } catch (ClassNotFoundException e) {
            log.warn("Class '{}' hasn't been found!", className, e);
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

    @Deprecated
    public boolean getEnableDefaultTyping() {
        return DefaultTypingMode.ENABLE.equals(getDefaultTypingMode());
    }

    @Deprecated
    public void setEnableDefaultTyping(boolean enableDefaultTyping) {
        if (enableDefaultTyping) {
            setDefaultTypingMode(DefaultTypingMode.ENABLE);
        } else {
            setDefaultTypingMode(DefaultTypingMode.SMART);
        }
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
}
