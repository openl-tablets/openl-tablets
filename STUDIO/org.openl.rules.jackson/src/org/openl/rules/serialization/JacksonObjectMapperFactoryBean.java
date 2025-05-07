package org.openl.rules.serialization;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.xml.bind.annotation.XmlSeeAlso;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.Builder;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import groovy.lang.GroovyObject;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.ruleservice.databinding.annotation.JacksonBindingConfigurationUtils;
import org.openl.rules.ruleservice.databinding.annotation.MixInClass;
import org.openl.rules.ruleservice.databinding.annotation.MixInClassFor;
import org.openl.rules.serialization.jackson.SubtypeMixin;
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
import org.openl.rules.variation.NoVariation;
import org.openl.rules.variation.Variation;
import org.openl.rules.variation.VariationsResult;
import org.openl.util.StringUtils;
import org.openl.util.generation.InterfaceTransformer;

public class JacksonObjectMapperFactoryBean implements JacksonObjectMapperFactory {

    private static final AtomicLong incrementer = new AtomicLong();

    private static final Class<?>[] VARIATION_CLASSES = new Class[]{Variation.class,
            NoVariation.class,
            ArgumentReplacementVariation.class,
            ComplexVariation.class,
            DeepCloningVariation.class,
            JXPathVariation.class,
            VariationsResult.class};

    private static final DefaultTypingMode DEFAULT_VALUE_FOR_DEFAULT_TYPING_MODE = DefaultTypingMode.JAVA_LANG_OBJECT;

    private final Logger log = LoggerFactory.getLogger(JacksonObjectMapperFactoryBean.class);

    private boolean supportVariations = false;

    private DefaultTypingMode defaultTypingMode = DEFAULT_VALUE_FOR_DEFAULT_TYPING_MODE;

    private DateFormat defaultDateFormat = new StdDateFormat();

    private JsonInclude.Include serializationInclusion;

    private Set<String> overrideTypes;

    private Set<Class<?>> overrideClasses;

    private boolean failOnUnknownProperties = false;

    private boolean failOnEmptyBeans = SerializationFeature.FAIL_ON_EMPTY_BEANS.enabledByDefault();

    private boolean polymorphicTypeValidation = false;

    private boolean caseInsensitiveProperties = MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES.enabledByDefault();

    private ClassLoader classLoader;

    @Deprecated
    private Boolean simpleClassNameAsTypingPropertyValue;

    private JsonTypeInfo.Id jsonTypeInfoId = JsonTypeInfo.Id.CLASS;

    private String typingPropertyName = JsonTypeInfo.Id.CLASS.getDefaultPropertyName();

    private Class<?> enhanceMixInClassWithSubTypes(Class<?> classFor,
                                                   Class<?> originalMixInClass,
                                                   Set<Class<?>> classes,
                                                   ClassLoader classLoader) {
        Class<?> originalClass = originalMixInClass;
        if (originalClass == null) {
            originalClass = SubtypeMixin.class;
        }
        List<Class<?>> subTypeClasses = new ArrayList<>();
        Class<?> parentTypeClass = null;
        for (Class<?> x : classes) {
            if (x.getSuperclass() == classFor) {
                subTypeClasses.add(x);
            }
            if (x == classFor.getSuperclass()) {
                parentTypeClass = x;
            }
        }
        String className = classFor.getName() + "$EnhancedMixInClassWithSubTypes$" + incrementer.getAndIncrement();
        ClassWriter classWriter = new ClassWriter(0);
        String typingPropertyName = StringUtils.isNotBlank(
                getTypingPropertyName()) ? getTypingPropertyName() : JsonTypeInfo.Id.CLASS.getDefaultPropertyName();
        if (DefaultTypingMode.DISABLED.equals(getDefaultTypingMode())) {
            typingPropertyName = null;
        }
        ClassVisitor classVisitor = new SubtypeMixInClassWriter(classWriter,
                originalClass,
                parentTypeClass,
                subTypeClasses.toArray(new Class<?>[0]),
                Boolean.TRUE.equals(isSimpleClassNameAsTypingPropertyValue()) && JsonTypeInfo.Id.CLASS
                        .equals(getJsonTypeInfoId()) ? JsonTypeInfo.Id.NAME : getJsonTypeInfoId(),
                typingPropertyName);
        InterfaceTransformer transformer = new InterfaceTransformer(originalClass, className);
        transformer.accept(classVisitor);
        classWriter.visitEnd();
        try {
            return ClassLoaderUtils.defineClass(className, classWriter.toByteArray(), classLoader);
        } catch (Exception e1) {
            throw new IllegalStateException(e1);
        }
    }

    public ObjectMapper createJacksonObjectMapper() throws ClassNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTimeZone(TimeZone.getDefault());

        TypeFactory typeFactory = TypeFactory.defaultInstance().withClassLoader(getClassLoader());
        mapper.setTypeFactory(typeFactory);

        mapper.enable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS);
        mapper.registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());

        mapper.registerModule(new SimpleModule()
                .addSerializer(Double.class, new DoubleSerializer(Double.class))
                .addSerializer(Double.TYPE, new DoubleSerializer(Double.TYPE))
                .addSerializer(Float.class, new FloatSerializer())
                .addSerializer(Float.TYPE, new FloatSerializer()));

        AnnotationIntrospector primaryIntrospector = new JacksonAnnotationIntrospector();
        var secondaryIntrospector = new JakartaXmlBindAnnotationIntrospector(
                TypeFactory.defaultInstance());

        if (serializationInclusion != null) {
            mapper.setSerializationInclusion(serializationInclusion);
            secondaryIntrospector.setNonNillableInclusion(serializationInclusion);
        }

        AnnotationIntrospector introspector = new AnnotationIntrospectorPair(primaryIntrospector,
                secondaryIntrospector);

        mapper.setAnnotationIntrospector(introspector);

        Builder basicPolymorphicTypeValidatorBuilder = null;
        final boolean polymorphicTypeValidation = isPolymorphicTypeValidation();
        if (polymorphicTypeValidation) {
            basicPolymorphicTypeValidatorBuilder = BasicPolymorphicTypeValidator.builder();
            basicPolymorphicTypeValidatorBuilder.allowIfSubTypeIsArray();
            basicPolymorphicTypeValidatorBuilder.allowIfBaseType(IRulesRuntimeContext.class);
            basicPolymorphicTypeValidatorBuilder.allowIfSubType(IRulesRuntimeContext.class);
            basicPolymorphicTypeValidatorBuilder.allowIfBaseType(DefaultRulesRuntimeContext.class);
            basicPolymorphicTypeValidatorBuilder.allowIfSubType(DefaultRulesRuntimeContext.class);
        }

        Set<Class<?>> overrideClasses = extractOverrideClasses(basicPolymorphicTypeValidatorBuilder,
                polymorphicTypeValidation);

        for (Class<?> clazz : getConfigurationClasses()) {
            MixInClassFor mixInClass = clazz.getAnnotation(MixInClassFor.class);
            if (mixInClass != null) {
                Arrays.stream(mixInClass.value()).forEach(forClass -> mapper.addMixIn(forClass, clazz));
            }
            MixInClass mixInRulesClass = clazz.getAnnotation(MixInClass.class);
            if (mixInRulesClass != null) {
                Arrays.stream(mixInRulesClass.types()).forEach(forClass -> mapper.addMixIn(forClass, clazz));
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

        if (!DefaultTypingMode.DISABLED.equals(getDefaultTypingMode())) {
            ObjectMapper.DefaultTyping defaultTyping = null;
            switch (getDefaultTypingMode()) {
                case NON_FINAL:
                    defaultTyping = ObjectMapper.DefaultTyping.NON_FINAL;
                    break;
                case OBJECT_AND_NON_CONCRETE:
                    defaultTyping = ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE;
                    break;
                case NON_CONCRETE_AND_ARRAYS:
                    defaultTyping = ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS;
                    break;
                case JAVA_LANG_OBJECT:
                    defaultTyping = ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT;
                    break;
                case EVERYTHING:
                    defaultTyping = ObjectMapper.DefaultTyping.EVERYTHING;
                    break;
            }
            mapper.activateDefaultTypingAsProperty(
                    polymorphicTypeValidation ? basicPolymorphicTypeValidatorBuilder.build()
                            : LaissezFaireSubTypeValidator.instance,
                    defaultTyping,
                    StringUtils.isNotBlank(getTypingPropertyName()) ? getTypingPropertyName()
                            : JsonTypeInfo.Id.CLASS.getDefaultPropertyName());
        } else {
            mapper.deactivateDefaultTyping();
        }

        mapper.addMixIn(GroovyObject.class, org.openl.rules.serialization.jackson.groovy.lang.GroovyObject.class);

        if (isSupportVariations()) {
            addMixIn(mapper, Variation.class, VariationType.class);
            addMixIn(mapper, ArgumentReplacementVariation.class, ArgumentReplacementVariationType.class);
            addMixIn(mapper, ComplexVariation.class, ComplexVariationType.class);
            addMixIn(mapper, DeepCloningVariation.class, DeepCloningVariationType.class);
            addMixIn(mapper, JXPathVariation.class, JXPathVariationType.class);
            addMixIn(mapper, VariationsResult.class, VariationsResultType.class);
            Collections.addAll(overrideClasses, VARIATION_CLASSES);
            if (polymorphicTypeValidation) {
                basicPolymorphicTypeValidatorBuilder.allowIfBaseType(Variation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfSubType(Variation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfBaseType(ArgumentReplacementVariation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfSubType(ArgumentReplacementVariation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfBaseType(ComplexVariation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfSubType(ComplexVariation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfBaseType(DeepCloningVariation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfSubType(DeepCloningVariation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfBaseType(JXPathVariation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfSubType(JXPathVariation.class);
                basicPolymorphicTypeValidatorBuilder.allowIfBaseType(VariationsResult.class);
                basicPolymorphicTypeValidatorBuilder.allowIfSubType(VariationsResult.class);
            }
        }

        for (Class<?> clazz : overrideClasses) {
            Class<?> subtypeMixInClass = enhanceMixInClassWithSubTypes(clazz,
                    mapper.findMixInClassFor(clazz),
                    overrideClasses,
                    getClassLoader());
            mapper.addMixIn(clazz, subtypeMixInClass);
        }

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, isFailOnUnknownProperties());
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, isCaseInsensitiveProperties());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, isFailOnEmptyBeans());
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, false); // OpenL uses ENUMs names
        mapper.configure(MapperFeature.ALLOW_EXPLICIT_PROPERTY_RENAMING, true);
        mapper.disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
        mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        mapper.disable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE);
        mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        if (getDefaultDateFormat() != null) {
            mapper.setDateFormat(getDefaultDateFormat());
        }

        return mapper;
    }

    private Set<Class<?>> extractOverrideClasses(Builder basicPolymorphicTypeValidatorBuilder,
                                                 boolean polymorphicTypeValidation) throws ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        if (getOverrideTypes() != null) {
            for (String className : getOverrideTypes()) {
                Class<?> clazz = loadClass(className);
                registerOverrideClass(basicPolymorphicTypeValidatorBuilder, polymorphicTypeValidation, classes, clazz);
            }
        }
        if (getOverrideClasses() != null) {
            for (Class<?> clazz : getOverrideClasses()) {
                registerOverrideClass(basicPolymorphicTypeValidatorBuilder, polymorphicTypeValidation, classes, clazz);
            }
        }
        if (isSupportVariations()) {
            for (Class<?> clazz : VARIATION_CLASSES) {
                registerOverrideClass(basicPolymorphicTypeValidatorBuilder, polymorphicTypeValidation, classes, clazz);
            }
        }
        return classes;
    }

    private List<Class<?>> getConfigurationClasses() throws ClassNotFoundException {
        List<Class<?>> configurationClasses = new ArrayList<>();
        if (getOverrideTypes() != null) {
            for (String className : getOverrideTypes()) {
                Class<?> clazz = loadClass(className);
                if (JacksonBindingConfigurationUtils.isConfiguration(clazz)) {
                    configurationClasses.add(clazz);
                }
            }
        }
        if (getOverrideClasses() != null) {
            for (Class<?> clazz : getOverrideClasses()) {
                if (JacksonBindingConfigurationUtils.isConfiguration(clazz)) {
                    configurationClasses.add(clazz);
                }
            }
        }
        return configurationClasses;
    }

    private void registerOverrideClass(Builder basicPolymorphicTypeValidatorBuilder,
                                       boolean polymorphicTypeValidation,
                                       Set<Class<?>> classes,
                                       Class<?> clazz) {
        if (!classes.contains(clazz)) {
            if (!JacksonBindingConfigurationUtils.isConfiguration(clazz)) {
                classes.add(clazz);
                if (polymorphicTypeValidation) {
                    basicPolymorphicTypeValidatorBuilder.allowIfBaseType(clazz);
                    basicPolymorphicTypeValidatorBuilder.allowIfSubType(clazz);
                }
                XmlSeeAlso xmlSeeAlso = clazz.getAnnotation(XmlSeeAlso.class);
                if (xmlSeeAlso != null) {
                    for (Class<?> cls : xmlSeeAlso.value()) {
                        registerOverrideClass(basicPolymorphicTypeValidatorBuilder,
                                polymorphicTypeValidation,
                                classes,
                                cls);
                    }
                }
            }
        }
    }

    private void addMixIn(ObjectMapper mapper, Class<?> classFor, Class<?> mixIn) {
        if (mapper.findMixInClassFor(classFor) == null) {
            mapper.addMixIn(classFor, mixIn);
        }
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException {
        return getClassLoader().loadClass(className);
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
            this.defaultTypingMode = DEFAULT_VALUE_FOR_DEFAULT_TYPING_MODE;
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

    public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
        this.failOnEmptyBeans = failOnEmptyBeans;
    }

    public boolean isFailOnEmptyBeans() {
        return failOnEmptyBeans;
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

    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            return Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Set<Class<?>> getOverrideClasses() {
        return overrideClasses;
    }

    public void setOverrideClasses(Set<Class<?>> overrideClasses) {
        this.overrideClasses = overrideClasses;
    }

    public boolean isCaseInsensitiveProperties() {
        return caseInsensitiveProperties;
    }

    public void setCaseInsensitiveProperties(boolean caseInsensitiveProperties) {
        this.caseInsensitiveProperties = caseInsensitiveProperties;
    }

    public String getTypingPropertyName() {
        return typingPropertyName;
    }

    public void setTypingPropertyName(String typingPropertyName) {
        this.typingPropertyName = typingPropertyName;
    }

    public Boolean isSimpleClassNameAsTypingPropertyValue() {
        return simpleClassNameAsTypingPropertyValue;
    }

    public void setSimpleClassNameAsTypingPropertyValue(Boolean simpleClassNameAsTypingPropertyValue) {
        this.simpleClassNameAsTypingPropertyValue = simpleClassNameAsTypingPropertyValue;
    }

    public JsonTypeInfo.Id getJsonTypeInfoId() {
        return jsonTypeInfoId;
    }

    public void setJsonTypeInfoId(JsonTypeInfo.Id jsonTypeInfoId) {
        if (jsonTypeInfoId == null) {
            throw new IllegalArgumentException("jsonTypeInfoId cannot be null");
        }
        this.jsonTypeInfoId = jsonTypeInfoId;
    }
}
