package org.openl.rules.serialization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.springframework.core.env.Environment;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.model.RulesDeployHelper;
import org.openl.rules.serialization.jackson.NonNullMixIn;
import org.openl.types.IOpenClass;
import org.openl.util.StringUtils;
import org.openl.util.generation.InterfaceTransformer;

public class ProjectJacksonObjectMapperFactoryBean implements JacksonObjectMapperFactory {

    private static final AtomicLong incrementer = new AtomicLong();

    public static final String ROOT_CLASS_NAMES_BINDING_OLD = "rootClassNamesBinding";
    public static final String SUPPORT_VARIATIONS = "ruleservice.isSupportVariations";
    public static final String ROOT_CLASS_NAMES_BINDING = "databinding.rootClassNames";
    public static final String JACKSON_CASE_INSENSITIVE_PROPERTIES = "jackson.caseInsensitiveProperties";
    public static final String JACKSON_DEFAULT_DATE_FORMAT = "jackson.defaultDateFormat";
    public static final String JACKSON_DEFAULT_TYPING_MODE = "jackson.defaultTypingMode";
    public static final String JACKSON_SERIALIZATION_INCLUSION = "jackson.serializationInclusion";
    public static final String JACKSON_FAIL_ON_UNKNOWN_PROPERTIES = "jackson.failOnUnknownProperties";
    public static final String JACKSON_FAIL_ON_EMPTY_BEANS = "jackson.failOnEmptyBeans";
    public static final String JACKSON_SIMPLE_CLASS_NAME_AS_TYPING_PROPERTY_VALUE = "jackson.simpleClassNameAsTypingPropertyValue";
    public static final String JACKSON_JSON_TYPE_INFO_ID = "jackson.jsonTypeInfoId";
    public static final String JACKSON_TYPING_PROPERTY_NAME = "jackson.typingPropertyName";
    public static final String JACKSON_PROPERTY_NAMING_STRATEGY = "jackson.propertyNamingStrategy";

    private final JacksonObjectMapperFactoryBean delegate = new JacksonObjectMapperFactoryBean();

    private XlsModuleOpenClass xlsModuleOpenClass;

    private RulesDeploy rulesDeploy;
    private Environment environment;

    public RulesDeploy getRulesDeploy() {
        return rulesDeploy;
    }

    public void setRulesDeploy(RulesDeploy rulesDeploy) {
        this.rulesDeploy = rulesDeploy;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setXlsModuleOpenClass(XlsModuleOpenClass xlsModuleOpenClass) {
        this.xlsModuleOpenClass = xlsModuleOpenClass;
    }

    private DefaultTypingMode toDefaultTypingMode(String defaultTypingMode) {
        if (DefaultTypingMode.DISABLED.name().equalsIgnoreCase(defaultTypingMode.trim())) {
            return DefaultTypingMode.DISABLED;
        } else if (DefaultTypingMode.OBJECT_AND_NON_CONCRETE.name().equalsIgnoreCase(defaultTypingMode.trim())) {
            return DefaultTypingMode.OBJECT_AND_NON_CONCRETE;
        } else if (DefaultTypingMode.EVERYTHING.name().equalsIgnoreCase(defaultTypingMode.trim())) {
            return DefaultTypingMode.EVERYTHING;
        } else if (DefaultTypingMode.NON_CONCRETE_AND_ARRAYS.name().equalsIgnoreCase(defaultTypingMode.trim())) {
            return DefaultTypingMode.NON_CONCRETE_AND_ARRAYS;
        } else if (DefaultTypingMode.JAVA_LANG_OBJECT.name().equalsIgnoreCase(defaultTypingMode.trim())) {
            return DefaultTypingMode.JAVA_LANG_OBJECT;
        } else if (DefaultTypingMode.NON_FINAL.name().equalsIgnoreCase(defaultTypingMode.trim())) {
            return DefaultTypingMode.NON_FINAL;
        }
        throw new ObjectMapperConfigurationParsingException(String.format(
                "Expected JAVA_LANG_OBJECT/OBJECT_AND_NON_CONCRETE/NON_CONCRETE_AND_ARRAYS/NON_FINAL/EVERYTHING/DISABLED value for '%s' in the configuration for service '%s'.",
                JACKSON_DEFAULT_TYPING_MODE,
                getRulesDeploy().getServiceName()));
    }

    protected void applyProjectConfiguration() {
        if (rulesDeploy != null && Boolean.TRUE.equals(rulesDeploy.isProvideVariations())
                || environment != null && Boolean.TRUE.equals(environment.getProperty(SUPPORT_VARIATIONS, Boolean.class))) {
            delegate.setSupportVariations(true);
        }

        processJacksonPropertiesSettingBoolean(getProperty(JACKSON_CASE_INSENSITIVE_PROPERTIES),
                JACKSON_CASE_INSENSITIVE_PROPERTIES,
                delegate::setCaseInsensitiveProperties);
        processJacksonPropertiesSettingBoolean(getProperty(JACKSON_FAIL_ON_UNKNOWN_PROPERTIES),
                JACKSON_FAIL_ON_UNKNOWN_PROPERTIES,
                delegate::setFailOnUnknownProperties);
        processJacksonPropertiesSettingBoolean(getProperty(JACKSON_FAIL_ON_EMPTY_BEANS),
                JACKSON_FAIL_ON_EMPTY_BEANS,
                delegate::setFailOnEmptyBeans);

        processJacksonDefaultDateFormatSetting(getProperty(JACKSON_DEFAULT_DATE_FORMAT));
        processJacksonDefaultTypingModeSetting(getProperty(JACKSON_DEFAULT_TYPING_MODE));
        processJacksonSerializationInclusionSetting(getProperty(JACKSON_SERIALIZATION_INCLUSION));
        processJacksonTypingPropertyNameSetting(getProperty(JACKSON_TYPING_PROPERTY_NAME));
        processJacksonSimpleClassNameAsTypingPropertyValueSetting(getProperty(JACKSON_SIMPLE_CLASS_NAME_AS_TYPING_PROPERTY_VALUE));
        processJacksonJsonTypeInfoIdSetting(getProperty(JACKSON_JSON_TYPE_INFO_ID));
        processRootClassNamesBindingSetting(getProperty(ROOT_CLASS_NAMES_BINDING));
        processXlsModuleOpenClassRelatedSettings();
    }

    protected JacksonObjectMapperFactoryBean getDelegate() {
        return delegate;
    }

    protected Object getProperty(String name) {
        Object result = null;
        if (rulesDeploy != null && rulesDeploy.getConfiguration() != null) {
            result = rulesDeploy.getConfiguration().get(name);
            if (result == null && name.equals(ROOT_CLASS_NAMES_BINDING)) {
                result = rulesDeploy.getConfiguration().get(ROOT_CLASS_NAMES_BINDING_OLD);
            }
        }
        if (result == null && environment != null) {
            return environment.getProperty("ruleservice." + name);
        }
        return result;
    }

    private void processXlsModuleOpenClassRelatedSettings() {
        if (xlsModuleOpenClass != null) {
            Set<Class<?>> rootClassNamesBindingClasses = delegate.getOverrideClasses();
            if (rootClassNamesBindingClasses == null) {
                rootClassNamesBindingClasses = new HashSet<>();
            } else {
                rootClassNamesBindingClasses = new HashSet<>(rootClassNamesBindingClasses);
            }
            for (IOpenClass type : xlsModuleOpenClass.getTypes()) {
                if (type instanceof DatatypeOpenClass) {
                    rootClassNamesBindingClasses.add(type.getInstanceClass());
                } else if (type instanceof CustomSpreadsheetResultOpenClass) {
                    rootClassNamesBindingClasses.add(((CustomSpreadsheetResultOpenClass) type).getBeanClass());
                }
            }
            for (CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass : xlsModuleOpenClass
                    .getCombinedSpreadsheetResultOpenClasses()) {
                rootClassNamesBindingClasses.add(customSpreadsheetResultOpenClass.getBeanClass());
            }
            // Check: custom spreadsheet is enabled
            if (xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
                rootClassNamesBindingClasses
                        .add(xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                .toCustomSpreadsheetResultOpenClass()
                                .getBeanClass());
            }
            delegate.setOverrideClasses(rootClassNamesBindingClasses);
        }
    }

    protected void processJacksonPropertiesSettingBoolean(Object value, String property, Consumer<Boolean> consumer) {
        if (value != null) {
            if (value instanceof Boolean) {
                consumer.accept((Boolean) value);
            } else if (value instanceof String) {
                consumer.accept(Boolean.parseBoolean((String) value));
            } else {
                throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected true/false value for '%s' in the configuration for service '%s'.",
                                property,
                                rulesDeploy.getServiceName()));
            }
        }
    }

    protected void processJacksonSimpleClassNameAsTypingPropertyValueSetting(
            Object simpleClassNameAsTypingPropertyValue) {
        if (simpleClassNameAsTypingPropertyValue != null) {
            if (simpleClassNameAsTypingPropertyValue instanceof Boolean) {
                delegate.setSimpleClassNameAsTypingPropertyValue((Boolean) simpleClassNameAsTypingPropertyValue);
            } else if (simpleClassNameAsTypingPropertyValue instanceof String) {
                delegate.setSimpleClassNameAsTypingPropertyValue(
                        Boolean.parseBoolean((String) simpleClassNameAsTypingPropertyValue));
            } else {
                throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected true/false value for '%s' in the configuration for service '%s'.",
                                JACKSON_SIMPLE_CLASS_NAME_AS_TYPING_PROPERTY_VALUE,
                                rulesDeploy.getServiceName()));
            }
        }
    }

    protected void processJacksonSerializationInclusionSetting(Object serializationInclusion) {
        if (serializationInclusion != null) {
            if (serializationInclusion instanceof String) {
                String stringValue = (String) serializationInclusion;
                try {
                    delegate.setSerializationInclusion(JsonInclude.Include.valueOf(stringValue));
                } catch (IllegalArgumentException e) {
                    throw new ObjectMapperConfigurationParsingException(String.format(
                            "Invalid serializationInclusion value is used for '%s' in the configuration for service '%s'.",
                            JACKSON_SERIALIZATION_INCLUSION,
                            rulesDeploy.getServiceName()), e);
                }
            } else {
                throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected string value for '%s' in the configuration for service '%s'.",
                                JACKSON_SERIALIZATION_INCLUSION,
                                rulesDeploy.getServiceName()));
            }
        }
    }

    protected void processRootClassNamesBindingSetting(Object rootClassNamesBinding) {
        if (rootClassNamesBinding instanceof String) {
            Set<String> rootClassNamesBindingClassNames = delegate.getOverrideTypes();
            if (rootClassNamesBindingClassNames == null) {
                rootClassNamesBindingClassNames = new HashSet<>();
            } else {
                rootClassNamesBindingClassNames = new HashSet<>(rootClassNamesBindingClassNames);
            }
            rootClassNamesBindingClassNames
                    .addAll(RulesDeployHelper.splitRootClassNamesBindingClasses((String) rootClassNamesBinding));
            delegate.setOverrideTypes(rootClassNamesBindingClassNames);
        } else {
            if (rootClassNamesBinding != null) {
                throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected string value for '%s' in the configuration for service '%s'.",
                                ROOT_CLASS_NAMES_BINDING,
                                rulesDeploy.getServiceName()));
            }
        }
    }

    protected void processJacksonTypingPropertyNameSetting(Object typingPropertyName) {
        if (typingPropertyName instanceof String) {
            String stringValue = (String) typingPropertyName;
            delegate.setTypingPropertyName(stringValue);
        } else {
            if (typingPropertyName != null) {
                throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected string value for '%s' in the configuration for service '%s'.",
                                JACKSON_TYPING_PROPERTY_NAME,
                                rulesDeploy.getServiceName()));
            }
        }
    }

    protected void processJacksonJsonTypeInfoIdSetting(Object jsonTypeInfoId) {
        if (jsonTypeInfoId != null) {
            if (jsonTypeInfoId instanceof JsonTypeInfo.Id) {
                delegate.setJsonTypeInfoId((JsonTypeInfo.Id) jsonTypeInfoId);
            } else if (jsonTypeInfoId instanceof String) {
                JsonTypeInfo.Id jtiId = JsonTypeInfo.Id.valueOf(((String) jsonTypeInfoId).trim());
                delegate.setJsonTypeInfoId(jtiId);
            } else {
                throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected string value for '%s' in the configuration for service '%s'.",
                                JACKSON_JSON_TYPE_INFO_ID,
                                rulesDeploy.getServiceName()));
            }
        }
    }

    protected void processJacksonDefaultTypingModeSetting(Object defaultTypingMode) {
        if (defaultTypingMode != null) {
            if (defaultTypingMode instanceof DefaultTypingMode) {
                delegate.setDefaultTypingMode((DefaultTypingMode) defaultTypingMode);
            } else if (defaultTypingMode instanceof String) {
                DefaultTypingMode dtm = toDefaultTypingMode((String) defaultTypingMode);
                if (dtm != null) {
                    delegate.setDefaultTypingMode(dtm);
                }
            } else {
                throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected string value for '%s' in the configuration for service '%s'.",
                                JACKSON_DEFAULT_TYPING_MODE,
                                rulesDeploy.getServiceName()));
            }
        }
    }

    protected void processJacksonDefaultDateFormatSetting(Object defaultDateFormat) {
        if (defaultDateFormat != null) {
            if (defaultDateFormat instanceof String) {
                String defaultDateFormatString = (String) defaultDateFormat;
                if (StringUtils.isNotBlank(defaultDateFormatString)) {
                    try {
                        delegate.setDefaultDateFormat(new ExtendedStdDateFormat(defaultDateFormatString));
                    } catch (Exception e) {
                        throw new ObjectMapperConfigurationParsingException(
                                String.format("Invalid date format is used in the configuration for service '%s'.",
                                        rulesDeploy.getServiceName()),
                                e);
                    }
                }
            } else {
                throw new ObjectMapperConfigurationParsingException(
                        String.format("Expected string value for '%s' in the configuration for service '%s'.",
                                JACKSON_DEFAULT_DATE_FORMAT,
                                rulesDeploy.getServiceName()));
            }
        }
    }

    public final ObjectMapper createJacksonObjectMapper() throws ClassNotFoundException {
        applyBeforeProjectConfiguration();
        applyProjectConfiguration();
        applyAfterProjectConfiguration();
        return enhanceObjectMapper(delegate.createJacksonObjectMapper());
    }

    private static String getRootName(ObjectMapper objectMapper, Class<?> cls) {
        AnnotationIntrospector introspector = objectMapper.getSerializationConfig().getAnnotationIntrospector();
        BeanDescription beanDesc = objectMapper.getSerializationConfig().introspectClassAnnotations(cls);
        AnnotatedClass ac = beanDesc.getClassInfo();
        PropertyName propertyName = introspector.findRootName(ac);
        if (propertyName != null && propertyName.hasSimpleName()) {
            return propertyName.getSimpleName();
        } else {
            return cls.getSimpleName();
        }
    }

    private void forEachType(XlsModuleOpenClass xlsModuleOpenClass,
                             Consumer<DatatypeOpenClass> datatypeOpenClassConsumer,
                             Consumer<CustomSpreadsheetResultOpenClass> customSpreadsheetResultOpenClassConsumer) {
        for (IOpenClass type : xlsModuleOpenClass.getTypes()) {
            if (type instanceof DatatypeOpenClass) {
                datatypeOpenClassConsumer.accept((DatatypeOpenClass) type);
            } else if (type instanceof CustomSpreadsheetResultOpenClass) {
                customSpreadsheetResultOpenClassConsumer.accept((CustomSpreadsheetResultOpenClass) type);
            }
        }
        // Check: custom spreadsheet is enabled
        if (xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
            for (CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass : xlsModuleOpenClass
                    .getCombinedSpreadsheetResultOpenClasses()) {
                customSpreadsheetResultOpenClassConsumer.accept(customSpreadsheetResultOpenClass);
            }
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = xlsModuleOpenClass
                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass();
            customSpreadsheetResultOpenClassConsumer.accept(customSpreadsheetResultOpenClass);
        }
    }

    private void processTypesFromXlsModuleOpenClass(ObjectMapper objectMapper, XlsModuleOpenClass xlsModuleOpenClass) {
        Map<Class<?>, String> clsToRootName = new HashMap<>();

        forEachType(xlsModuleOpenClass,
                e -> clsToRootName.put(e.getInstanceClass(), getRootName(objectMapper, e.getInstanceClass())),
                e -> clsToRootName.put(e.getBeanClass(), getRootName(objectMapper, e.getBeanClass())));

        Map<Class<?>, String> conflictedRootNames = new HashMap<>();
        for (Class<?> cls : clsToRootName.keySet()) {
            if (clsToRootName.values().stream().filter(e -> Objects.equals(e, clsToRootName.get(cls))).count() > 1) {
                conflictedRootNames.put(cls, cls.getName());
            }
        }
        forEachType(xlsModuleOpenClass,
                e -> addMixInAnnotationsToDatatype(objectMapper, e, conflictedRootNames.get(e.getInstanceClass())),
                e -> addMixInAnnotationsToSprBeanClass(objectMapper, e, conflictedRootNames.get((e.getBeanClass()))));
    }

    protected ObjectMapper enhanceObjectMapper(ObjectMapper objectMapper) {
        if (xlsModuleOpenClass != null) {
            PropertyNamingStrategy propertyNamingStrategy = extractPropertyNamingStrategy();
            if (propertyNamingStrategy != null) {
                objectMapper.setPropertyNamingStrategy(propertyNamingStrategy);
            }
            processTypesFromXlsModuleOpenClass(objectMapper, xlsModuleOpenClass);
            xlsModuleOpenClass.getExternalXlsModuleOpenClasses()
                    .forEach(e -> processTypesFromXlsModuleOpenClass(objectMapper, e));
        }
        return objectMapper;
    }

    private PropertyNamingStrategy extractPropertyNamingStrategy() {
        return extractPropertyNamingStrategy(rulesDeploy, getClassLoader());
    }

    public static PropertyNamingStrategy extractPropertyNamingStrategy(RulesDeploy rulesDeploy,
                                                                       ClassLoader classLoader) {
        if (rulesDeploy != null) {
            if (rulesDeploy.getConfiguration() != null) {
                Object propertyNamingStrategy = rulesDeploy.getConfiguration().get(JACKSON_PROPERTY_NAMING_STRATEGY);
                if (propertyNamingStrategy != null) {
                    if (propertyNamingStrategy instanceof String) {
                        String propertyNamingStrategyClassName = (String) propertyNamingStrategy;
                        try {
                            Class<?> propertyNamingStrategyClass = classLoader
                                    .loadClass(propertyNamingStrategyClassName);
                            if (!PropertyNamingStrategy.class.isAssignableFrom(propertyNamingStrategyClass)) {
                                throw new ObjectMapperConfigurationParsingException(String.format(
                                        "Failed to load property name strategy class '%s' for service '%s'. The class must be an implementation of interface '%s'.",
                                        JACKSON_PROPERTY_NAMING_STRATEGY,
                                        rulesDeploy.getServiceName(),
                                        PropertyNamingStrategy.class.getTypeName()));
                            }
                            try {
                                return (PropertyNamingStrategy) propertyNamingStrategyClass.newInstance();
                            } catch (InstantiationException | IllegalAccessException e) {
                                throw new ObjectMapperConfigurationParsingException(String.format(
                                        "Failed to instantiate property name strategy class '%s' for service '%s'.",
                                        JACKSON_PROPERTY_NAMING_STRATEGY,
                                        rulesDeploy.getServiceName()), e);
                            }
                        } catch (ClassNotFoundException e) {
                            throw new ObjectMapperConfigurationParsingException(
                                    String.format("Failed to load property naming strategy class '%s' for service '%s'.",
                                            JACKSON_PROPERTY_NAMING_STRATEGY,
                                            rulesDeploy.getServiceName()),
                                    e);
                        }
                    } else {
                        throw new ObjectMapperConfigurationParsingException(
                                String.format("Expected string value for '%s' in the configuration for service '%s'.",
                                        JACKSON_PROPERTY_NAMING_STRATEGY,
                                        rulesDeploy.getServiceName()));
                    }
                }
            }
        }
        return null;
    }

    private void addMixInAnnotationsToSprBeanClass(ObjectMapper objectMapper,
                                                   CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
                                                   String rootName) {
        Class<?> sprBeanClass = customSpreadsheetResultOpenClass.getBeanClass();
        Class<?> originalMixInClass = objectMapper.findMixInClassFor(sprBeanClass);
        Class<?> mixInClass = enhanceMixInClassForSprBeanClass(
                originalMixInClass != null ? originalMixInClass : NonNullMixIn.class,
                rootName,
                getClassLoader());
        objectMapper.addMixIn(sprBeanClass, mixInClass);
    }

    private void addMixInAnnotationsToDatatype(ObjectMapper objectMapper,
                                               DatatypeOpenClass datatypeOpenClass,
                                               String rootName) {
        Class<?> originalMixInClass = objectMapper.findMixInClassFor(datatypeOpenClass.getInstanceClass());
        Class<?> mixInClass = enhanceMixInClassForDatatypeClass(
                originalMixInClass != null ? originalMixInClass : NonNullMixIn.class,
                rootName,
                getClassLoader());
        objectMapper.addMixIn(datatypeOpenClass.getInstanceClass(), mixInClass);
    }

    private Class<?> enhanceMixInClassForSprBeanClass(Class<?> originalMixInClass,
                                                      String rootName,
                                                      ClassLoader classLoader) {
        String className = originalMixInClass.getName() + "$EnhancedMixInClassForSprBeanClass$" + incrementer
                .getAndIncrement();
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new SpreadsheetResultBeanClassMixInAnnotationsWriter(classWriter,
                className,
                originalMixInClass,
                rootName);
        return defineAndLoadClass(originalMixInClass, classLoader, className, classWriter, classVisitor);
    }

    private Class<?> enhanceMixInClassForDatatypeClass(Class<?> originalMixInClass,
                                                       String rootName,
                                                       ClassLoader classLoader) {
        String className = originalMixInClass.getName() + "$EnhancedMixInClassForDatatypeClass$" + incrementer
                .getAndIncrement();
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new DatatypeOpenClassMixInAnnotationsWriter(classWriter,
                className,
                originalMixInClass,
                rootName);
        return defineAndLoadClass(originalMixInClass, classLoader, className, classWriter, classVisitor);
    }

    private Class<?> defineAndLoadClass(Class<?> originalMixInClass,
                                        ClassLoader classLoader,
                                        String className,
                                        ClassWriter classWriter,
                                        ClassVisitor classVisitor) {
        InterfaceTransformer transformer = new InterfaceTransformer(originalMixInClass, className);
        transformer.accept(classVisitor);
        classWriter.visitEnd();
        try {
            return ClassLoaderUtils.defineClass(className, classWriter.toByteArray(), classLoader);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }

    protected void applyBeforeProjectConfiguration() {
    }

    protected void applyAfterProjectConfiguration() {
        delegate.setPolymorphicTypeValidation(true);
    }

    private ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }

    public void setClassLoader(ClassLoader classLoader) {
        delegate.setClassLoader(classLoader);
    }
}
